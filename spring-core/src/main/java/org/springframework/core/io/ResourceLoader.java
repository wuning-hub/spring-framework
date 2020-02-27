/*
 * Copyright 2002-2018 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.util.ResourceUtils;

/**
 * Strategy interface for loading resources (e.. class path or file system
 * resources). An {@link org.springframework.context.ApplicationContext}
 * is required to provide this functionality, plus extended
 * {@link org.springframework.core.io.support.ResourcePatternResolver} support.
 *
 * <p>{@link DefaultResourceLoader} is a standalone implementation that is
 * usable outside an ApplicationContext, also used by {@link ResourceEditor}.
 *
 * <p>Bean properties of type Resource and Resource array can be populated
 * from Strings when running in an ApplicationContext, using the particular
 * context's resource loading strategy.
 *
 * @author Juergen Hoeller
 * @see Resource
 * @see org.springframework.core.io.support.ResourcePatternResolver
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.ResourceLoaderAware
 * @since 10.03.2004
 */

/**
 * @author 注释作者Ning
 * @Date 2020/2/27 18:52
 * Resource 定义了统一的资源,那么资源的加载则由ResourceLoader 来统一定义
 *
 *  ResourceLoader为spring的资源加载的统一抽象,具体资源的加载由相应的实现类来完成,  所以给他起个名字  统一资源定位器
 *  其实名字无所谓  只要明白他的作用就OK
 *  资源加载器 顾名思义 就是根据给定资源的文件地址,返回一个具体的对应的Resource
 */

public interface ResourceLoader {

	/** Pseudo URL prefix for loading from the class path: "classpath:". */
	//假的url 前缀 可看ResourceUtils  估计后面要用吧
	String CLASSPATH_URL_PREFIX = ResourceUtils.CLASSPATH_URL_PREFIX;


	/**
	 * 根据给定资源位置返回一个资源句柄
	 * Return a Resource handle for the specified resource location.
	 * <p>The handle should always be a reusable resource descriptor,
	 * allowing for multiple {@link Resource#getInputStream()} calls.
	 * <p><ul>
	 * <li>Must support fully qualified URLs, e.g. "file:C:/test.dat".
	 * <li>Must support classpath pseudo-URLs, e.g. "classpath:test.dat".
	 * <li>Should support relative file paths, e.g. "WEB-INF/test.dat".
	 * 这里就是给了一个规则  给你说 这个方法支持了些啥子路径
	 * URL位置资源，如 "file:C:/test.dat" 。
	 * ClassPath位置资源，如 "classpath:test.dat 。
	 * 相对路径资源，如 "WEB-INF/test.dat" ，此时返回的Resource 实例，根据实现不同而不同。
	 *
	 *下面这句话,里面终于有见过的了
	 * 特定实现  通常由SpringApplication 实现
	 * (This will be implementation-specific, typically provided by an
	 * ApplicationContext implementation.)
	 * </ul>
	 * 这里需要注意的是 :返回的资源句柄  并一定是存在的,还得调用Resource 的exists 方法来验证一哈
	 * <p>Note that a Resource handle does not imply an existing resource;
	 * you need to invoke {@link Resource#exists} to check for existence.
	 * @param location the resource location
	 * @return a corresponding Resource handle (never {@code null})
	 * @see #CLASSPATH_URL_PREFIX
	 * @see Resource#exists()
	 * @see Resource#getInputStream()
	 *
	 * 我其实特别想知道 这个getResource 方法里面 是怎么搞的（后面你就会知道 这是重中之重）
	 */
	Resource getResource(String location);

	/**
	 * Expose the ClassLoader used by this ResourceLoader.
	 * 就是告诉你 ResourceLoader使用的什么ClassLoader
	 * 此处复习一个知识  java 的类加载器有哪些？
	 *  1.启动（BootStrap）类加载器
	 *  就是加载Jvm自身需要的类，C++实现的，是虚拟机自身的一部分，这里就有问题了 我把jar包扔到指定目录下（指定目录
	 *  就是<JAVA_HOME>/lib路径下的核心类库或-Xbootclasspath参数指定的路径）去，虚拟机会不会加载呢？并不会，虚拟机是根据
	 *  设定好的名字来的，比如java/sun/javax 开头的，所以啊，改成这几个名字 应该会加载 ，此处有疑问啊
	 *
	 *  2.扩展（Extension）类加载器
	 *  扩展类加载器，后娘养的那个 被人家归过来那个，起名字都不给起个差不多的，加载<JAVA_HOME>/lib/ext目录下
	 *  或者由系统变量-Djava.ext.dir指定位路径中的类库
	 *
	 *  3.系统（System）类加载器
	 *  加载系统类路径java -classpath或-D java.class.path 指定路径下的类库  也就是默认类加载器，
	 *  直接ClassLoader#getSystemClassLoader()就可以拿到
	 *
	 *  4.对了 还有自定义类加载器
	 *
	 *  这3个加载器并不是说一个加载另外俩个就不干了，相互配合来的
	 *  另外要注意的是Java虚拟机对class文件是按需加载的 说人话就是什么时候需要 使用这个类 才会去加载
	 *  （将class文件加载成class对象） 就是渣男 懂了伐
	 *
	 *  好了 重点来了 就是java 虚拟机采用的双亲委派模式  下面说人话讲一下
	 *  说白了就是坑爹模式  层层坑爹  一个类加载器收到类加载请求后，这货并不会去加载，而且给他父类加载 父类如果有爹的话 就开始
	 *  迭代了  实在是 解决不了了  这个儿子才会去搞，让我们一般讲  这就很难受啊  可是实际并不是这样
	 *
	 *  那么 重点来了 坑爹有什么好！！！
	 *  俩个好处
	 *  1.避免重复加载  父类已经加载了 子类没必要再加载一次
	 *  2.安全 防止核心API库被随意篡改  举个例子  我自己搞一个java.lang.Integer的类 通过双亲委派模式 给类加载器，类加载器发现
	 *  这个玩意已经加载过了  直接就返回原来已经加载得Interger.class
	 *  此处应该想到得是  我弄一个伪类 java.lang.SingleInterge类(这个类是胡编的) 放到classpath路径下呢 由于父类加载器下并没有
	 *  这个类 然后就给了子类去搞 最终会通过系统类加载器去加载。但是这样做是不允许，因为java.lang是核心API包，需要访问权限，
	 *  强制加载将会报出如下异常  java.lang.SecurityException: Prohibited package name: java.lang
	 *
	 *  具体的代码层级得东西  去百度
	 *
	 *
	 * <p>Clients which need to access the ClassLoader directly can do so
	 * in a uniform manner with the ResourceLoader, rather than relying
	 * on the thread context ClassLoader.
	 *
	 * 在分析 Resource 时，提到了一个类 ClassPathResource ，这个类是可以根据指定的 ClassLoader 来加载资源的。
	 * @return the ClassLoader
	 * 系统类加载器不行
	 * (only {@code null} if even the system ClassLoader isn't accessible)
	 * @see org.springframework.util.ClassUtils#getDefaultClassLoader()
	 * @see org.springframework.util.ClassUtils#forName(String, ClassLoader)
	 */
	@Nullable
	ClassLoader getClassLoader();

	//说完ResourceLoader接口  说ResourceLoader的具体实现  跳 DefaultResourceLoader

}
