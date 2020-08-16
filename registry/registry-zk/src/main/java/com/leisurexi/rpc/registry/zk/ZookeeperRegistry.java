package com.leisurexi.rpc.registry.zk;

import com.leisurexi.rpc.common.protocol.RpcProtocol;
import com.leisurexi.rpc.common.registry.Registry;
import com.leisurexi.rpc.common.util.RegistryUtils;
import com.leisurexi.rpc.common.util.ServiceKeyUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
    private final static byte[] PROVIDER_OFFLINE = new byte[]{0};

    /**
     * 正常在线服务
     */
    private final static byte[] PROVIDER_ONLINE = new byte[]{1};

    /**
     * 保存服务发布者url
     */
    private ConcurrentHashMap<String, List<String>> providerUrls = new ConcurrentHashMap<>();

    /**
     * 保存服务消费者url
     */
    private ConcurrentHashMap<String, String> consumerUrls = new ConcurrentHashMap<>();

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
                    String providerPath = RegistryUtils.buildProviderPath(ZookeeperConstant.ZK_REGISTRY_PATH, serviceInfo[0]);
                    RpcProtocol rpcProtocol = RpcProtocol.builder()
                            .uuid(UUID.randomUUID().toString())
                            .host(inetAddress[0])
                            .port(Integer.parseInt(inetAddress[1]))
                            .serviceName(serviceInfo[0])
                            .version(serviceInfo[1])
                            .build();
                    zkClient.create().creatingParentContainersIfNeeded()
                            .withMode(CreateMode.EPHEMERAL)
                            .forPath(providerPath + "/" + rpcProtocol.hashCode(), rpcProtocol.toJsonString().getBytes());
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
                String providerPath = RegistryUtils.buildProviderPath(ZookeeperConstant.ZK_REGISTRY_PATH, serviceKey);
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
    public List<RpcProtocol> discovery(String serviceName) {
        String providerPath = RegistryUtils.buildProviderPath(ZookeeperConstant.ZK_REGISTRY_PATH, serviceName);
        try {
            // 获取所有的服务提供者 url 列表
            List<String> nodeList = zkClient.getChildren().forPath(providerPath);
            if (CollectionUtils.isEmpty(nodeList)) {
                throw new IllegalStateException("No provider for " + serviceName);
            }
            List<RpcProtocol> serverList = nodeList.stream().map(nodeName -> {
                try {
                    byte[] bytes = zkClient.getData().forPath(providerPath + "/" + nodeName);
                    String json = new String(bytes);
                    RpcProtocol rpcProtocol = RpcProtocol.fromJson(json);
                    return rpcProtocol;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());
            return serverList;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() {
        if (zkClient != null) {
            zkClient.close();
        }
    }

}
