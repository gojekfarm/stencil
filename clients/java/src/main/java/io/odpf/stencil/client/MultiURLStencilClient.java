package io.odpf.stencil.client;

import com.google.protobuf.Descriptors;
import io.odpf.stencil.cache.DescriptorCacheLoader;
import io.odpf.stencil.config.StencilConfig;
import io.odpf.stencil.models.DescriptorAndTypeName;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
* {@link StencilClient} implementation that can fetch descriptor sets from multiple URLs
*/
public class MultiURLStencilClient implements Serializable, StencilClient {

    private List<StencilClient> stencilClients;
    private boolean shouldAutoRefreshCache;

    /**
     * @param urls List of URLs to fetch protobuf descriptor sets from
     * @param config Stencil configs
     * @param cacheLoader Extension of Guava {@link com.google.common.cache.CacheLoader} for Proto Descriptor sets
     */
    public MultiURLStencilClient(List<String> urls, StencilConfig config, DescriptorCacheLoader cacheLoader) {
        shouldAutoRefreshCache = config.getCacheAutoRefresh();
        stencilClients = urls.stream().map(url -> new URLStencilClient(url, config, cacheLoader)).collect(Collectors.toList());
    }

    @Override
    public Descriptors.Descriptor get(String protoClassName) {
        Optional<StencilClient> requiredStencil = stencilClients.stream().filter(stencilClient -> stencilClient.get(protoClassName) != null).findFirst();
        return requiredStencil.map(stencilClient -> stencilClient.get(protoClassName)).orElse(null);
    }

    @Override
    public Map<String, Descriptors.Descriptor> getAll() {
        Map<String, Descriptors.Descriptor> requiredStencil = new HashMap<>();
        stencilClients.stream().map(StencilClient::getAll)
                .forEach(requiredStencil::putAll);
        return requiredStencil;
    }

    @Override
    public Map<String, String> getTypeNameToPackageNameMap() {
        Map<String, String> requiredStencil = new HashMap<>();
        stencilClients.stream().map(StencilClient::getTypeNameToPackageNameMap)
                .forEach(requiredStencil::putAll);
        return requiredStencil;
    }

    @Override
    public Map<String, DescriptorAndTypeName> getAllDescriptorAndTypeName() {
        Map<String, DescriptorAndTypeName> requiredStencil = new HashMap<>();
        stencilClients.stream().map(StencilClient::getAllDescriptorAndTypeName)
                .forEach(requiredStencil::putAll);
        return requiredStencil;
    }

    @Override
    public void close() {
        stencilClients.forEach(c -> {
            try {
                c.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void refresh() {
        stencilClients.forEach(c -> {
            c.refresh();
        });
    }

    @Override
    public boolean shouldAutoRefreshCache() {
        return shouldAutoRefreshCache;
    }
}
