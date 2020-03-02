/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.io;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * Default implementation of the {@link ResourceLoader} interface.
 * Used by {@link ResourceEditor}, and serves as base class for
 * {@link org.springframework.context.support.AbstractApplicationContext}.
 * Can also be used standalone.
 *
 * <p>Will return a {@link UrlResource} if the location value is a URL,
 * and a {@link ClassPathResource} if it is a non-URL path or a
 * "classpath:" pseudo-URL.
 *
 * @author Juergen Hoeller
 * @since 10.03.2004
 * @see FileSystemResourceLoader
 * @see org.springframework.context.support.ClassPathXmlApplicationContext
 */

/**
 * @author 注释作者Ning
 * @Date 2020/2/27 20:06
 *
 * 这个玩意 看名字就知道了 是ResourceLoader 的默认实现了
 */
public class DefaultResourceLoader implements ResourceLoader {

	/**
	 *   一上来就   private ClassLoader classLoader，是不是很熟，兄弟  你猜对了 马上空参 带参  get  set  就来了
	 *   下面直接跳过  去重点 getResource（）
	 */

	@Nullable
	private ClassLoader classLoader;

	//ProtocolResolver 集合
	private final Set<ProtocolResolver> protocolResolvers = new LinkedHashSet<>(4);

	private final Map<Class<?>, Map<Resource, ?>> resourceCaches = new ConcurrentHashMap<>(4);

	/**
	 * Create a new DefaultResourceLoader.
	 * <p>ClassLoader access will happen using the thread context class loader
	 * at the time of this ResourceLoader's initialization.
	 * 初始化的时候 默认使用的是  thread context class loader    也就是 Thread.currentThread()#getContextClassLoader()这个
	 * 人家注释写的很明白哈  看！！！！
	 * @see java.lang.Thread#getContextClassLoader()
	 */
	public DefaultResourceLoader() {

		this.classLoader = ClassUtils.getDefaultClassLoader();
	}



	/**
	 * Create a new DefaultResourceLoader.
	 * @param classLoader the ClassLoader to load class path resources with, or {@code null}
	 * for using the thread context class loader at the time of actual resource access
	 *在使用带参数的构造函数时，可以通过 ClassUtils#getDefaultClassLoader()获取。这句话复制的
	 *
	 *  ClassLoader defaultClassLoader = ClassUtils.getDefaultClassLoader();
	 */
	public DefaultResourceLoader(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}


	/**
	 * Specify the ClassLoader to load class path resources with, or {@code null}
	 * for using the thread context class loader at the time of actual resource access.
	 * <p>The default is that ClassLoader access will happen using the thread context
	 * class loader at the time of this ResourceLoader's initialization.
	 */
	public void setClassLoader(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * Return the ClassLoader to load class path resources with.
	 * <p>Will get passed to ClassPathResource's constructor for all
	 * ClassPathResource objects created by this resource loader.
	 * @see ClassPathResource
	 */
	@Override
	@Nullable
	public ClassLoader getClassLoader() {
		return (this.classLoader != null ? this.classLoader : ClassUtils.getDefaultClassLoader());
	}

	/**
	 *
	 *
	 * Register the given resolver with this resource loader, allowing for
	 * additional protocols to be handled.
	 * <p>Any such resolver will be invoked ahead of this loader's standard
	 * resolution rules. It may therefore also override any default rules.
	 * @since 4.3
	 * @see #getProtocolResolvers()
	 */
	public void addProtocolResolver(ProtocolResolver resolver) {
		Assert.notNull(resolver, "ProtocolResolver must not be null");
		this.protocolResolvers.add(resolver);
	}

	/**
	 * 返回当前注册解析器的结合  允许自行更正与修改
	 * Return the collection of currently registered protocol resolvers,
	 * allowing for introspection as well as modification.
	 * @since 4.3
	 */
	public Collection<ProtocolResolver> getProtocolResolvers() {
		return this.protocolResolvers;
	}

	/**
	 * Obtain a cache for the given value type, keyed by {@link Resource}.
	 * @param valueType the value type, e.g. an ASM {@code MetadataReader}
	 * @return the cache {@link Map}, shared at the {@code ResourceLoader} level
	 * @since 5.0
	 */
	@SuppressWarnings("unchecked")
	public <T> Map<Resource, T> getResourceCache(Class<T> valueType) {
		return (Map<Resource, T>) this.resourceCaches.computeIfAbsent(valueType, key -> new ConcurrentHashMap<>());
	}

	/**
	 * Clear all resource caches in this resource loader.
	 * @since 5.0
	 * @see #getResourceCache
	 */
	public void clearResourceCaches() {
		this.resourceCaches.clear();
	}



	/**
	 * 这个没注释  我来写（重点来了）
	 * @param location the resource location
	 * @return
	 *
	 * DefaultResourceLoader 的子类 并没有对这个方法实现进行重写覆盖,所以可以断定
	 * ResourceLoader 的核心资源加载策略就封装在这里
	 *
	 */
	@Override
	public Resource getResource(String location) {
		/**
		 * 一上来断言搞一下 判断location不能为空  顺便把这个断言说一下
		 * Assert:  很像if()else{}  但是 不一样的是
		 * 大多数情况下，我们要进行验证的假设，只是属于偶然性事件，又或者我们仅仅想测试一下，一些最坏情况是否发生，所以这里有了 assert()。
		 * assert 宏的原型定义在 assert.h 中，其作用是如果它的条件返回错误，则终止程序执行。
		 * 开销比较大 而且只在debug生效好像  就是调试用的
		*/
		Assert.notNull(location, "Location must not be null");

		for (ProtocolResolver protocolResolver : getProtocolResolvers()) {
			// 首先通过ProtocolResolver(已经拟定的解析器)来 解析资源地址 以获得资源
			Resource resource = protocolResolver.resolve(location, this);
			if (resource != null) {
				return resource;
			}
		}

		if (location.startsWith("/")) {
			//如果location以"/"开头  获得资源 其实
			//getResourceByPath 调用 ClassPathContextResource
			//ClassPathContextResource 调用 其父类 ClassPathResource的 ClassPathResource方法
			// 然后再StringUtils.cleanPath方法  清理路径
			//然后再截取1位 再拿着类加载器 去加载 里面具体是个三元表达式
			return getResourceByPath(location);
		}
		else if (location.startsWith(CLASSPATH_URL_PREFIX)) {
			//开头是否为 classpath:
			return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()), getClassLoader());
		}
		else {
			try {
				// Try to parse the location as a URL...
				//// 然后，根据是否为文件 URL ，是则返回 FileUrlResource 类型的资源，否则返回 UrlResource 类型的资源
				URL url = new URL(location);
				return (ResourceUtils.isFileURL(url) ? new FileUrlResource(url) : new UrlResource(url));
			}
			catch (MalformedURLException ex) {
				// 最后，返回 ClassPathContextResource 类型的资源
				// No URL -> resolve as resource path.
				return getResourceByPath(location);
			}
		}
	}
	/**
	 * @author 注释作者Ning
	 * @Date 2020/3/2 18:17
	 * 好了,整体说一下getResource()方法,
	 * 1.首先通过ProtocolResolver来加载资源,成功的话就返回Resource
	 * 2.其次,若location以"/"开头,则调用#getResourceByPath()方法,构造ClassPathResource类型资源返回,在构造资源的时候
	 * 通过getClassloader()获取当前的ClassLoader.
	 * 3.然后,构造URL,尝试通过它进行资源定位,若没有抛出MalformedURLException异常,则判断是否为FileURL,
	 * 如果是就构造FileUrlResource,否则构造UrlResource类型的资源.
	 * 4.最后在加载过程中抛出MalformedURLException异常,则委派getResourceByPath()方法,实现资源定位
	 */


	/**
	 * Return a Resource handle for the resource at the given path.
	 * <p>The default implementation supports class path locations. This should
	 * be appropriate for standalone implementations but can be overridden,
	 * e.g. for implementations targeted at a Servlet container.
	 * @param path the path to the resource
	 * @return the corresponding Resource handle
	 * @see ClassPathResource
	 * @see org.springframework.context.support.FileSystemXmlApplicationContext#getResourceByPath
	 * @see org.springframework.web.context.support.XmlWebApplicationContext#getResourceByPath
	 */
	protected Resource getResourceByPath(String path) {
		return new ClassPathContextResource(path, getClassLoader());
	}


	/**
	 * ClassPathResource that explicitly expresses a context-relative path
	 * through implementing the ContextResource interface.
	 */
	protected static class ClassPathContextResource extends ClassPathResource implements ContextResource {

		public ClassPathContextResource(String path, @Nullable ClassLoader classLoader) {
			super(path, classLoader);
		}

		@Override
		public String getPathWithinContext() {
			return getPath();
		}

		@Override
		public Resource createRelative(String relativePath) {
			String pathToUse = StringUtils.applyRelativePath(getPath(), relativePath);
			return new ClassPathContextResource(pathToUse, getClassLoader());
		}
	}

}
