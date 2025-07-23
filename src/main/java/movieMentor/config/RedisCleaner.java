package movieMentor.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class RedisCleaner {

    private final CacheManager cacheManager;

    public RedisCleaner(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void clearRedisCacheOnStartup() {
        cacheManager.getCacheNames().forEach(name -> {
            if (cacheManager.getCache(name) != null) {
                cacheManager.getCache(name).clear();
            }
        });
        System.out.println("✅ Redis cache cleared on startup");
    }
}
