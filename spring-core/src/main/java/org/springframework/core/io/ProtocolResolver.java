/*
 * Copyright 2002-2016 the original author or authors.
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

/**
 * A resolution strategy for protocol-specific resource handles.
 *
 * <p>Used as an SPI for {@link DefaultResourceLoader}, allowing for
 * custom protocols to be handled without subclassing the loader
 * implementation (or application context implementation).
 *
 * @author Juergen Hoeller
 * @since 4.3
 * @see DefaultResourceLoader#addProtocolResolver
 */
/**
 * @author 注释作者Ning
 * @Date 2020/3/2 18:21
 *
 * @FunctionalInterface  函数式接口
 * 先复习一下什么是函数式接口?  1.有且仅有一个抽象方法  2.允许定义静态方法  3.允许定义默认方法
 * 4.允许java.lang.Object 中的public 方法
 * 注意的一点，该注解不是必须的，可以省略，前提是你的接口得符合函数式接口规范，写上只是为了编译器更方便进行检查，
 *如果编写的不是函数式接口，而加上 @FunctionalInterface接口，就会报错
 *
 * OK 言归正传
 *
 *
 * 用户自定义协议资源解决策略，是DefaultResourceLoader的SPI   怎么理解前面这个词呢，说人话的话，就是
 * 这个东西允许用户自定义资源加载协议，就可以不用去继承ResourceLoder了
 *
 * 前面看到Resource ,要实现自定义Resource,该怎么办，继承AbstractResource就可以了，但是现在有了ProtocolResolver
 * 同理 我们现在想拿到自定义的ResourceLoader ,得去实现DefaultResourceLoader，现在有了ProtocolResolver
 * 我们实现ProtocolResolver 同样可以实现自定义得ResourceLoader
 *
 *继续朝下走
 */
@FunctionalInterface
public interface ProtocolResolver {

	/**
	 *
	 * 就是通过给定得资源路径，使用指定得ResourceLoader,解析得到指定得location
	 * 如果成功的话，则返回对应的Resource
	 *
	 * Resolve the given location against the given resource loader
	 * if this implementation's protocol matches.
	 * @param location the user-specified resource location
	 * @param resourceLoader the associated resource loader
	 * @return a corresponding {@code Resource} handle if the given location
	 * matches this resolver's protocol, or {@code null} otherwise
	 *
	 *看一下ProtocolResolver，你就发现，他是没有实现类的，那么问题来了？
	 * ProtocolResolver没有实现了 那么他是怎么融入Spring体系中的，继续
	 *
	 * 跳DefaultResourceLoader#addProtocolResolver(ProtocolResolver resolver)方法
	 *
	 */
	@Nullable
	Resource resolve(String location, ResourceLoader resourceLoader);

}
