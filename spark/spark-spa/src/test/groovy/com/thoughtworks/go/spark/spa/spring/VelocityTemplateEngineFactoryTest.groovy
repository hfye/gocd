/*
 * Copyright 2018 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.spark.spa.spring

import com.thoughtworks.go.CurrentGoCDVersion
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple
import com.thoughtworks.go.server.service.RailsAssetsService
import com.thoughtworks.go.server.service.SecurityService
import com.thoughtworks.go.server.service.VersionInfoService
import com.thoughtworks.go.server.service.WebpackAssetsService
import com.thoughtworks.go.server.service.plugins.builder.DefaultPluginInfoFinder
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService
import com.thoughtworks.go.server.service.support.toggle.Toggles
import com.thoughtworks.go.spark.spa.RolesControllerDelegate
import org.apache.commons.lang3.StringEscapeUtils
import org.jsoup.Jsoup
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.io.DefaultResourceLoader
import spark.ModelAndView

import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.Mockito.mock
import static org.mockito.MockitoAnnotations.initMocks

class VelocityTemplateEngineFactoryTest {

  InitialContextProvider initialContextProvider
  private VelocityTemplateEngineFactory engine

  @BeforeEach
  void setUp() {
    initMocks(this)
    Toggles.initializeWith(mock(FeatureToggleService.class))
    initialContextProvider = new InitialContextProvider(mock(RailsAssetsService.class), mock(WebpackAssetsService), mock(SecurityService), mock(VersionInfoService), mock(DefaultPluginInfoFinder))
    engine = new VelocityTemplateEngineFactory(initialContextProvider, new DefaultResourceLoader(getClass().getClassLoader()), "classpath:velocity")
    engine.afterPropertiesSet()
    SessionUtils.setCurrentUser(new GoUserPrinciple("bob", "Bob"))
  }

  @Test
  void 'it should render a basic template'() {
    def output = engine.create(RolesControllerDelegate.class, "layouts/test-layout.vm")
      .render(new ModelAndView(Collections.emptyMap(), "templates/test-template.vm"))
    assertThat(output)
      .contains("begin parent layout")
      .contains("this is the actual template content")
      .contains("end parent layout")
  }

  @Test
  void 'it should escape html entities by default'() {
    def userInput = "<script>alert('i can has hax')</script>"
    def output = engine.create(RolesControllerDelegate.class, "layouts/test-layout.vm")
      .render(new ModelAndView(Collections.singletonMap("user-input", userInput), "templates/escape-html-entities.vm"))
    assertThat(output)
      .contains("begin parent layout")
      .contains(StringEscapeUtils.escapeHtml4(userInput))
      .contains("end parent layout")
  }

  @Test
  void 'it should render footer links'() {
    def output = engine.create(RolesControllerDelegate.class, "layouts/single_page_app.vm")
      .render(new ModelAndView(Collections.emptyMap(), "templates/test-template.vm"))

    def document = Jsoup.parse(output)
    def footer = document.selectFirst(".app-footer")
    assertThat(footer.text()).contains("Copyright © ${Calendar.getInstance().get(Calendar.YEAR)} ThoughtWorks, Inc.")
    assertThat(footer.select("a").eachAttr('href')).isEqualTo([
      "https://www.thoughtworks.com/products",
      "https://www.apache.org/licenses/LICENSE-2.0",
      "/go/assets/dependency-license-report-${CurrentGoCDVersion.getInstance().fullVersion()}",
      "https://twitter.com/goforcd",
      "https://github.com/gocd/gocd",
      "https://groups.google.com/d/forum/go-cd",
      "https://docs.gocd.org/${CurrentGoCDVersion.getInstance().goVersion()}",
      "https://www.gocd.org/plugins/",
      "https://api.gocd.org/${CurrentGoCDVersion.getInstance().goVersion()}",
      "/go/about",
      "/go/cctray.xml"
    ].collect { it.toString() })
  }
}
