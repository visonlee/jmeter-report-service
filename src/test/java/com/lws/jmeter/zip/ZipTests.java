package com.lws.jmeter.zip;

import com.lws.jmeter.util.CompressUtils;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

class ZipTests {

	@Test
	void test() throws Exception {
		//compress
		Path basePath = Paths.get("/Users/vince/Desktop/tmp/demo");


		Path source = basePath.resolve("zipTest");
		Path destination = basePath.resolve("archive.zip");
//
//		CompressUtils.compress(source, destination);

		// extract
		CompressUtils.extract(basePath.resolve("test.7z"), basePath.resolve("out"));
	}

}
