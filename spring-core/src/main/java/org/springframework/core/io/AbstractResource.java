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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.NestedIOException;
import org.springframework.lang.Nullable;
import org.springframework.util.ResourceUtils;

/**
 * Convenience base class for {@link Resource} implementations,
 * pre-implementing typical behavior.
 *
 * <p>The "exists" method will check whether a File or InputStream can
 * be opened; "isOpen" will always return false; "getURL" and "getFile"
 * throw an exception; and "toString" will return the description.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 28.12.2003
 * <p>
 * 默认抽象实现了Resource 接口,实现了其大部分的公共实现
 */
public abstract class AbstractResource implements Resource {

	/**
	 * This implementation checks whether a File can be opened,
	 * falling back to whether an InputStream can be opened.
	 * This will cover both directories and content resources.
	 */

	/**
	 * @author 注释作者Ning
	 * @Date 2020/2/26 19:57
	 */
	@Override
	public boolean exists() {
		//判断文件是否存在
		// Try file existence: can we find the file in the file system?
		if (isFile()) {
			//不存在的情况
			try {
				//基于File进行判断  抛出不能解析绝对路径的异常(进入File类调用SecurityManager继续判断,)
				return getFile().exists();
			} catch (IOException ex) {


				Log logger = LogFactory.getLog(getClass());
				if (logger.isDebugEnabled()) {
					logger.debug("Could not retrieve File for existence check of " + getDescription(), ex);
				}
			}
		}
		// Fall back to stream existence: can we open the stream?
		try {
			//再基于InputStream 进行判断
			getInputStream().close();
			return true;
		} catch (Throwable ex) {
			Log logger = LogFactory.getLog(getClass());
			if (logger.isDebugEnabled()) {
				logger.debug("Could not retrieve InputStream for existence check of " + getDescription(), ex);
			}
			return false;
		}
	}

	/**
	 * This implementation always returns {@code true} for a resource
	 * that {@link #exists() exists} (revised as of 5.1).
	 * 返回是否可读
	 */
	@Override
	public boolean isReadable() {
		return exists();
	}

	/**
	 * This implementation always returns {@code false}.
	 * 直接返回false 表示不能打开
	 */
	@Override
	public boolean isOpen() {
		return false;
	}

	/**
	 * This implementation always returns {@code false}.
	 * 直接返回false  不为文件
	 */
	@Override
	public boolean isFile() {
		return false;
	}

	/**
	 * This implementation throws a FileNotFoundException, assuming
	 * that the resource cannot be resolved to a URL.
	 * 假设 资源不能解析为url 直接抛出文件找不到异常
	 */
	@Override
	public URL getURL() throws IOException {
		throw new FileNotFoundException(getDescription() + " cannot be resolved to URL");
	}

	/**
	 * This implementation builds a URI based on the URL returned
	 * by {@link #getURL()}.
	 * 基于getURL 返回uri
	 */
	@Override
	public URI getURI() throws IOException {
		//假设url 能解析出来
		URL url = getURL();
		try {
			//看到这里 眼前一亮  迫不及待想去看一下 ResourceUtils 封装了什么 点进去有注释
			return ResourceUtils.toURI(url);
		} catch (URISyntaxException ex) {
			//nIvalid 什么鬼意思 我并不知道 只知道抛了一个io异常  以前写smm 框架好像见过这个嵌套异常
			throw new NestedIOException("nIvalid URI [" + url + "]", ex);
		}
	}

	/**
	 * This implementation throws a FileNotFoundException, assuming
	 * that the resource cannot be resolved to an absolute file path.
	 * 直接甩手掌柜  抛了异常出去给子类实现
	 */
	@Override
	public File getFile() throws IOException {
		throw new FileNotFoundException(getDescription() + " cannot be resolved to absolute file path");
	}

	/**
	 * This implementation returns {@link Channels#newChannel(InputStream)}
	 * with the result of {@link #getInputStream()}.
	 * <p>This is the same as in {@link Resource}'s corresponding default method
	 * but mirrored here for efficient JVM-level dispatching in a class hierarchy.
	 *
	 * 根据getInputStream的返回结果 构建ReadableByteChannel
	 * ReadableByteChannel接口继承Channel
	 */
	@Override
	public ReadableByteChannel readableChannel() throws IOException {
		return Channels.newChannel(getInputStream());
	}

	/**
	 * This implementation reads the entire InputStream to calculate the
	 * content length. Subclasses will almost always be able to provide
	 * a more optimal version of this, e.g. checking a File length.
	 *
	 * @see #getInputStream()
	 *
	 * 获取资源的长度
	 *
	 * 资源的长度其实就是资源字节的长度，代码就是while全部读取 然后累加长度
	 *
	 */
	@Override
	public long contentLength() throws IOException {
		InputStream is = getInputStream();
		try {
			long size = 0;
			byte[] buf = new byte[256]; //每次256字节
			int read;
			while ((read = is.read(buf)) != -1) {
				size += read;
			}
			return size;
		} finally {
			try {
				is.close();
			} catch (IOException ex) {
				Log logger = LogFactory.getLog(getClass());
				if (logger.isDebugEnabled()) {
					logger.debug("Could not close content-length InputStream for " + getDescription(), ex);
				}
			}
		}
	}

	/**
	 * This implementation checks the timestamp of the underlying File,
	 * if available.
	 *
	 * @see #getFileForLastModifiedCheck()
	 * 返回资源的最后修改时间
	 */
	@Override
	public long lastModified() throws IOException {
		File fileToCheck = getFileForLastModifiedCheck();
		//这里学到一个File 的新方法  返回文件最后一次修改时间
		long lastModified = fileToCheck.lastModified();
		if (lastModified == 0L && !fileToCheck.exists()) {
			throw new FileNotFoundException(getDescription() +
					" cannot be resolved in the file system for checking its last-modified timestamp");
		}
		return lastModified;
	}

	/**
	 * Determine the File to use for timestamp checking.
	 * <p>The default implementation delegates to {@link #getFile()}.
	 *
	 * @return the File to use for timestamp checking (never {@code null})
	 * @throws FileNotFoundException if the resource cannot be resolved as
	 *                               an absolute file path, i.e. is not available in a file system
	 * @throws IOException           in case of general resolution/reading failures
	 */
	protected File getFileForLastModifiedCheck() throws IOException {
		return getFile();
	}

	/**
	 * This implementation throws a FileNotFoundException, assuming
	 * that relative resources cannot be created for this resource.
	 *
	 * 抛出文件找不到异常，交给子类实现
	 */
	@Override
	public Resource createRelative(String relativePath) throws IOException {
		throw new FileNotFoundException("Cannot create a relative resource for " + getDescription());
	}

	/**
	 * This implementation always returns {@code null},
	 * assuming that this resource type does not have a filename.
	 * 没文件名永远返回null(又假设了  交给子类实现)
	 * 以后写接口  应该也可以这样模仿
	 */
	@Override
	@Nullable
	public String getFilename() {
		return null;
	}


	/**
	 * This implementation compares description strings.
	 *
	 *
	 * @see #getDescription()
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		//instanceof 二元运算符 左边是对象，右边是类；当对象是右边类或子类所创建对象时，返回true；否则，返回false
		return (this == other || (other instanceof Resource &&
				((Resource) other).getDescription().equals(getDescription())));
	}

	/**
	 * This implementation returns the description's hash code.
	 * 返回资源 hashcode
	 * @see #getDescription()
	 */
	@Override
	public int hashCode() {
		return getDescription().hashCode();
	}

	/**
	 * This implementation returns the description of this resource.
	 * 返回资源描述
	 * @see #getDescription()
	 */
	@Override
	public String toString() {
		return getDescription();
	}

/**
 * @author 注释作者Ning
 * @Date 2020/2/26 21:25
 *
 * 如果自己想实现自定义的Resource ，记住不要实现Resource 接口，而应该继承 AbstractResource 抽象类，
 * 然后根据当前的具体资源特性覆盖相应的方法即可。
 */


}
