package com.leisurexi.rpc.registry.zk;

import com.leisurexi.rpc.common.client.ProviderInfo;
import com.leisurexi.rpc.common.listener.ProviderInfoListener;
import com.leisurexi.rpc.common.registry.Registry;
import com.leisurexi.rpc.common.util.RegistryUtils;
import com.leisurexi.rpc.common.util.ServiceKeyUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static com.leisurexi.rpc.common.util.RegistryUtils.CONTEXT_SEP;
import static com.leisurexi.rpc.registry.zk.ZookeeperConstant.ZK_REGISTER_PATH;

/**
 * Zookeeper 服务注册中心实现
 *
 * @author: leisurexi
 * @date: 2020-08-13 2:04 下午
 */
@Slf4j
public class ZookeeperRegistry implements Registry {

    /**
     * Zookeeper 服务器地址
     */
    private String addr;

    /**
     * Zookeeper client
     */
    private CuratorFramework zkClient;

    /**
     * 服务被下线
     */
    private final static byte[] PROVIDER_OFFLINE = "0".getBytes();

    /**
     * 正常在线服务
     */
    private final static byte[] PROVIDER_ONLINE = "1".getBytes();

    /**
     * 保存服务发布者url
     */
    private ConcurrentHashMap<String, List<String>> providerUrls = new ConcurrentHashMap<>();

    /**
     * 保存服务消费者url
     */
    private ConcurrentHashMap<String, String> consumerUrls = new ConcurrentHashMap<>();

    /**
     * The provider info listener list
     */
    private List<ProviderInfoListener> listenerList = new CopyOnWriteArrayList<>();

    public ZookeeperRegistry(String addr) {
        this.addr = addr;
    }

    @Override
    public synchronized void init() {
        if (zkClient != null) {
            return;
        }
        if (StringUtils.isEmpty(addr)) {
            throw new IllegalArgumentException("addr must not be empty");
        }
        CuratorFrameworkFactory.Builder curatorFactory = CuratorFrameworkFactory.builder()
                .connectString(addr)
                .sessionTimeoutMs(ZookeeperConstant.ZK_SESSION_TIMEOUT)
                .connectionTimeoutMs(ZookeeperConstant.ZK_CONNECTION_TIMEOUT)
                .canBeReadOnly(false)
                .retryPolicy(new RetryNTimes(10, 5000))
                .defaultData(null);
        zkClient = curatorFactory.build();
    }

    @Override
    public synchronized boolean start() {
        if (zkClient == null) {
            log.warn("Start zookeeper registry must be do init first!");
            return false;
        }
        if (zkClient.getState() == CuratorFrameworkState.STARTED) {
            return true;
        }
        zkClient.start();
        return zkClient.getState() == CuratorFrameworkState.STARTED;
    }

    @Override
    public void register(String addr, Map<String, Object> serviceMap) {
        if (!CollectionUtils.isEmpty(serviceMap)) {
            serviceMap.forEach((serviceKey, obj) -> {
                try {
                    String[] inetAddress = addr.split(":");
                    String[] serviceInfo = serviceKey.split(ServiceKeyUtils.SERVICE_CONCAT_TOKEN);
                    // 服务节点地址
                    String providerPath = RegistryUtils.buildProviderPath(ZK_REGISTER_PATH, serviceInfo[0]);
                    // 监听节点下的变化
                    watchProviderNode(providerPath);
                    ProviderInfo providerInfo = ProviderInfo.builder()
                            .uuid(UUID.randomUUID().toString())
                            .host(inetAddress[0])
                            .port(Integer.parseInt(inetAddress[1]))
                            .serviceName(serviceInfo[0])
                            .version(serviceInfo[1])
                            .startTime(System.currentTimeMillis())
                            .build();
                    zkClient.create().creatingParentContainersIfNeeded()
                            .withMode(CreateMode.EPHEMERAL)
                            .forPath(providerPath + CONTEXT_SEP + providerInfo.toString(), PROVIDER_ONLINE);
                    log.info("Zookeeper [{}] 服务注册，地址: [{}]", serviceKey, addr);
                } catch (KeeperException.NodeExistsException e) {
                    // ignore
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            });
        }
    }

    @Override
    public void unRegister(String addr, Map<String, Object> serviceMap) {
        if (!CollectionUtils.isEmpty(serviceMap)) {
            serviceMap.forEach((serviceKey, obj) -> {
                String providerPath = RegistryUtils.buildProviderPath(ZK_REGISTER_PATH, serviceKey);
                try {
                    zkClient.delete().forPath(providerPath + addr);
                    log.info("Zookeeper [{}]，地址: [{}]，服务取消注册", serviceKey, addr);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Override
    public List<ProviderInfo> discovery(String serviceName) {
        String providerPath = RegistryUtils.buildProviderPath(ZK_REGISTER_PATH, serviceName);
        try {
            // 获取所有的服务提供者 url 列表
            List<String> nodeList = zkClient.getChildren().forPath(providerPath);
            if (CollectionUtils.isEmpty(nodeList)) {
                throw new IllegalStateException("No provider for " + serviceName);
            }
            List<ProviderInfo> serverList = nodeList.stream().map(node -> ProviderInfo.fromString(node)).collect(Collectors.toList());
            return serverList;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addProviderInfoListener(ProviderInfoListener providerInfoListener) {
        listenerList.add(providerInfoListener);
    }

    /**
     * 接口级别服务节点监听，并发送出相应的事件
     *
     * @param providerPath zk 中的服务节点路径
     */
    private void watchProviderNode(String providerPath) {
        /**
         * PathChildrenCache子节点缓存用于子节点的监听，监控本节点的子节点被创建、更新或者删除。需要强调两点：
         * 1.只能监听子节点，监听不到当前节点
         * 2.不能递归监听，子节点下的子节点不能递归监控
         */
        PathChildrenCache pathChildrenCache = new PathChildrenCache(zkClient, providerPath, true);
        pathChildrenCache.getListenable().addListener((curatorFramework, event) -> {
            if (log.isDebugEnabled()) {
                log.debug("Receive zookeeper event: type=[{}]", event.getType());
            }
            ChildData data = event.getData();
            switch (event.getType()) {
                // 新增接口级配置
                case CHILD_ADDED: {
                    if (log.isDebugEnabled()) {
                        log.debug("Child added event, providerPath: [{}], data: [{}]", providerPath, data);
                    }
                    ProviderInfo providerInfo = ZookeeperRegistryHelper.convertUrlToProvider(providerPath, data);
                    listenerList.stream().forEach(providerInfoListener -> providerInfoListener.addProvider(providerInfo));
                }
                break;
                case CHILD_REMOVED: {
                    if (log.isDebugEnabled()) {
                        log.debug("Child removed event, providerPath: [{}], data: [{}]", providerPath, data);
                    }
                    ProviderInfo providerInfo = ZookeeperRegistryHelper.convertUrlToProvider(providerPath, data);
                    listenerList.stream().forEach(providerInfoListener -> providerInfoListener.removeProvider(providerInfo));
                }
                break;
                case CHILD_UPDATED: {
                    if (log.isDebugEnabled()) {
                        log.debug("Child updated event, providerPath: [{}], data: [{}]", providerPath, data);
                    }
                    List<String> stringList = zkClient.getChildren().forPath(providerPath);
                    List<ProviderInfo> providerInfoList = stringList.stream().map(s -> ProviderInfo.fromString(s)).collect(Collectors.toList());
                    listenerList.stream().forEach(providerInfoListener -> providerInfoListener.updateProvider(providerInfoList));
                }
                break;
            }
        });
        try {
            pathChildrenCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void destroy() {
        if (zkClient != null) {
            zkClient.close();
        }
    }

}
