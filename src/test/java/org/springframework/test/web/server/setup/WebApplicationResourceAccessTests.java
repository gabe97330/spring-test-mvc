/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.web.server.setup;

import static org.springframework.test.web.server.request.MockHttpServletRequestBuilders.get;
import static org.springframework.test.web.server.result.MockMvcResultActions.*;
import static org.springframework.test.web.server.setup.MockMvcBuilders.annotationConfigMvcSetup;
import static org.springframework.test.web.server.setup.MockMvcBuilders.xmlConfigMvcSetup;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.server.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.servlet.view.UrlBasedViewResolver;
import org.springframework.web.servlet.view.tiles2.TilesConfigurer;
import org.springframework.web.servlet.view.tiles2.TilesView;

/**
 * Test access to web application resources through the MockServletContext.
 * For example Tiles configuration, serving resources like .js, .css, etc.
 * The WAR root may be file system or classpath-relative.
 */
@RunWith(Parameterized.class)
public class WebApplicationResourceAccessTests {

	@Parameters
	public static Collection<Object[]> parameters() {
		return Arrays.asList(new Object[][] {
				{ "src/test/webapp", true, false },
				{ "META-INF/web-resources", true, true },
				{ "src/test/webapp", false, false },
				{ "META-INF/web-resources", false, true }
		});
	}
	
	private MockMvc mockMvc;
	
	public WebApplicationResourceAccessTests(String webResourcePath, boolean isXmlConfig, boolean isClasspathRelative) {
		
		if (!isXmlConfig) {
			mockMvc = annotationConfigMvcSetup(TestConfiguration.class)
						.configureWarRootDir(webResourcePath, isClasspathRelative)
						.build();
		}
		else {
			String location = "classpath:org/springframework/test/web/server/setup/servlet-context.xml";
			mockMvc = xmlConfigMvcSetup(location)
						.configureWarRootDir(webResourcePath, isClasspathRelative)
						.build();
		}
	}
	
	@Test
	public void testWebResources() {

		// TilesView
		mockMvc.perform(get("/form"))
                .andExpect(response().status(200))
                .andExpect(response().forwardedUrl("/WEB-INF/layouts/main.jsp"));

		mockMvc.perform(get("/resources/Spring.js"))
				.andExpect(response().status(200))
				.andExpect(controller().controllerType(ResourceHttpRequestHandler.class))
				.andExpect(response().contentType("application/octet-stream"))
				.andExpect(response().responseBodyContains("Spring={};"));
		
		mockMvc.perform(get("/unknown/resource.js"))
			.andExpect(response().status(200))
			.andExpect(controller().controllerType(DefaultServletHttpRequestHandler.class))
			.andExpect(response().forwardedUrl("default"));
	}
	
	@Controller
	public static class TestController {

		@RequestMapping("/form")
		public void show() {
		}
	}

	@Configuration
	@EnableWebMvc
	static class TestConfiguration extends WebMvcConfigurerAdapter {

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			registry.addResourceHandler("/resources/**").addResourceLocations("/resources/");
		}

		@Override
		public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
			configurer.enable();
		}

		@Bean
		public UrlBasedViewResolver urlBasedViewResolver() {
			UrlBasedViewResolver resolver = new UrlBasedViewResolver();
			resolver.setViewClass(TilesView.class);
			return resolver;
		}
		
		@Bean
		public TilesConfigurer tilesConfigurer() {
			TilesConfigurer configurer = new TilesConfigurer();
			configurer.setDefinitions(new String[] {"/WEB-INF/**/tiles.xml"});
			return configurer;
		}
		
		@Bean
		public TestController testController() {
			return new TestController();
		}
	}
	
}
