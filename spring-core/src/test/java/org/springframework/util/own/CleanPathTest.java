package org.springframework.util.own;

import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;

/**
 * @author  Ning
 * @Date  2020/7/2 16:31
 **/
public class CleanPathTest {

	@Test
	public void cleanPath(){

		String a="D:\\conference_system\\src\\main\\java\\com\\starsee\\meeting\\conference\\config";
		System.out.println(StringUtils.cleanPath(a));
	}
}
