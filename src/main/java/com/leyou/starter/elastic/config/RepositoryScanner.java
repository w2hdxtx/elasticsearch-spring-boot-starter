package com.leyou.starter.elastic.config;

import com.leyou.starter.elastic.repository.Repository;
import com.leyou.starter.elastic.repository.RepositoryFactory;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RepositoryScanner implements BeanDefinitionRegistryPostProcessor, ResourceLoaderAware, ApplicationContextAware {
    private ApplicationContext applicationContext;

    private static final String DEFAULT_RESOURCE_PATTERN = "**/*.class";

    private MetadataReaderFactory metadataReaderFactory;

    private ResourcePatternResolver resourcePatternResolver;

    private RestHighLevelClient client;

    public RepositoryScanner(RestHighLevelClient client) {
        this.client = client;
    }
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
        // ????????????????????????
        List<String> packages = AutoConfigurationPackages.get(applicationContext);
        // ?????????????????????????????????
        Set<Class<?>> beanClazzSet = scannerPackages(packages.get(0));
        for (Class beanClazz : beanClazzSet) {
            // ???????????????repository
            if(isNotElasticsearchRepository(beanClazz)){
                continue;
            }
            // BeanDefinition?????????
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(beanClazz);
            GenericBeanDefinition definition = (GenericBeanDefinition) builder.getRawBeanDefinition();

            //?????????????????????????????????????????????????????????????????????
            definition.getConstructorArgumentValues().addGenericArgumentValue(beanClazz);
            definition.getConstructorArgumentValues().addIndexedArgumentValue(1, client);
            // ??????Bean??????
            definition.setBeanClass(RepositoryFactory.class);

            //??????????????????byType??????????????????????????????byName???
            definition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
            String simpleName = beanClazz.getSimpleName();
            simpleName = simpleName.substring(0, 1).toLowerCase() + simpleName.substring(1);
            beanDefinitionRegistry.registerBeanDefinition(simpleName, definition);
        }
    }

    private boolean isNotElasticsearchRepository(Class beanClazz) {
        return !beanClazz.isInterface() || beanClazz.getInterfaces().length <= 0 || beanClazz.getInterfaces()[0] != Repository.class;
    }

    /**
     * ????????????????????????????????????????????????
     * @param basePackage basePackage
     * @return Set<Class<?>> Set<Class<?>>
     */
    private Set<Class<?>> scannerPackages(String basePackage) {
        Set<Class<?>> set = new LinkedHashSet<>();
        String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                resolveBasePackage(basePackage) + '/' + DEFAULT_RESOURCE_PATTERN;
        try {
            Resource[] resources = this.resourcePatternResolver.getResources(packageSearchPath);
            for (Resource resource : resources) {
                if (resource.isReadable()) {
                    MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(resource);
                    String className = metadataReader.getClassMetadata().getClassName();
                    Class<?> clazz;
                    try {
                        clazz = Class.forName(className);
                        set.add(clazz);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return set;
    }

    private String resolveBasePackage(String basePackage) {
        return ClassUtils.convertClassNameToResourcePath(
                this.applicationContext.getEnvironment().resolveRequiredPlaceholders(basePackage));
    }



    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
        this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {

    }
}
