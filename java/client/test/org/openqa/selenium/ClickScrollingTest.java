// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.openqa.selenium;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.titleIs;
import static org.openqa.selenium.testing.Driver.ALL;
import static org.openqa.selenium.testing.Driver.CHROME;
import static org.openqa.selenium.testing.Driver.FIREFOX;
import static org.openqa.selenium.testing.Driver.HTMLUNIT;
import static org.openqa.selenium.testing.Driver.IE;
import static org.openqa.selenium.testing.Driver.MARIONETTE;
import static org.openqa.selenium.testing.Driver.SAFARI;
import static org.openqa.selenium.testing.TestUtilities.catchThrowable;

import org.junit.Test;
import org.openqa.selenium.interactions.MoveTargetOutOfBoundsException;
import org.openqa.selenium.testing.Ignore;
import org.openqa.selenium.testing.JUnit4TestBase;
import org.openqa.selenium.testing.SwitchToTopAfterTest;
import org.openqa.selenium.testing.drivers.Browser;

@Ignore(value = HTMLUNIT, reason = "Scrolling requires rendering")
public class ClickScrollingTest extends JUnit4TestBase {

  @Test
  public void testClickingOnAnchorScrollsPage() {
    String scrollScript = "";
    scrollScript += "var pageY;";
    scrollScript += "if (typeof(window.pageYOffset) == 'number') {";
    scrollScript += "  pageY = window.pageYOffset;";
    scrollScript += "} else {";
    scrollScript += "  pageY = document.documentElement.scrollTop;";
    scrollScript += "}";
    scrollScript += "return pageY;";

    driver.get(pages.macbethPage);

    driver.findElement(By.partialLinkText("last speech")).click();

    long yOffset = (Long) ((JavascriptExecutor) driver).executeScript(scrollScript);

    // Focusing on to click, but not actually following,
    // the link will scroll it in to view, which is a few pixels further than 0
    assertThat("Did not scroll", yOffset, is(greaterThan(300L)));
  }

  @Test
  public void testShouldScrollToClickOnAnElementHiddenByOverflow() {
    String url = appServer.whereIs("click_out_of_bounds_overflow.html");
    driver.get(url);

    WebElement link = driver.findElement(By.id("link"));
    link.click();
  }

  @Test
  @Ignore(MARIONETTE)
  public void testShouldBeAbleToClickOnAnElementHiddenByOverflow() {
    driver.get(appServer.whereIs("scroll.html"));

    WebElement link = driver.findElement(By.id("line8"));
    // This used to throw a MoveTargetOutOfBoundsException - we don't expect it to
    link.click();
    assertEquals("line8", driver.findElement(By.id("clicked")).getText());
  }

  @Test
  @Ignore(value = CHROME, reason = "failed")
  public void testShouldBeAbleToClickOnAnElementHiddenByDoubleOverflow() {
    driver.get(appServer.whereIs("scrolling_tests/page_with_double_overflow_auto.html"));

    driver.findElement(By.id("link")).click();
    onlyPassIfNotOnMac(662, () -> wait.until(titleIs("Clicked Successfully!")));
  }

  @Test
  @Ignore(value = SAFARI, reason = "failed")
  public void testShouldBeAbleToClickOnAnElementHiddenByYOverflow() {
    driver.get(appServer.whereIs("scrolling_tests/page_with_y_overflow_auto.html"));

    driver.findElement(By.id("link")).click();
    wait.until(titleIs("Clicked Successfully!"));
  }

  @Test
  @Ignore(value = IE, issue = "716")
  @Ignore(value = FIREFOX, issue = "716")
  @Ignore(value = MARIONETTE, issue = "https://github.com/mozilla/geckodriver/issues/915")
  @Ignore(value = SAFARI, reason = "not tested")
  public void testShouldBeAbleToClickOnAnElementPartiallyHiddenByOverflow() {
    driver.get(appServer.whereIs("scrolling_tests/page_with_partially_hidden_element.html"));

    driver.findElement(By.id("btn")).click();
    wait.until(titleIs("Clicked Successfully!"));
  }

  @Test
  public void testShouldNotScrollOverflowElementsWhichAreVisible() {
    driver.get(appServer.whereIs("scroll2.html"));
    WebElement list = driver.findElement(By.tagName("ul"));
    WebElement item = list.findElement(By.id("desired"));
    item.click();
    long yOffset =
        (Long)((JavascriptExecutor)driver).executeScript("return arguments[0].scrollTop;", list);
    assertEquals("Should not have scrolled", 0, yOffset);
  }

  @Test
  @Ignore(CHROME)
  @Ignore(value = SAFARI,
      reason = "Safari: button1 is scrolled to the bottom edge of the view, " +
               "so additonal scrolling is still required for button2")
  @Ignore(MARIONETTE)
  public void testShouldNotScrollIfAlreadyScrolledAndElementIsInView() {
    driver.get(appServer.whereIs("scroll3.html"));
    driver.findElement(By.id("button1")).click();
    long scrollTop = getScrollTop();
    driver.findElement(By.id("button2")).click();
    assertEquals(scrollTop, getScrollTop());
  }

  @Test
  public void testShouldBeAbleToClickRadioButtonScrolledIntoView() {
    driver.get(appServer.whereIs("scroll4.html"));
    driver.findElement(By.id("radio")).click();
    // If we don't throw, we're good
  }

  @Test
  @Ignore(value = IE, reason = "IE has special overflow handling")
  @Ignore(MARIONETTE)
  public void testShouldScrollOverflowElementsIfClickPointIsOutOfViewButElementIsInView() {
    driver.get(appServer.whereIs("scroll5.html"));
    driver.findElement(By.id("inner")).click();
    assertEquals("clicked", driver.findElement(By.id("clicked")).getText());
  }

  @SwitchToTopAfterTest
  @Test
  @Ignore(SAFARI)
  @Ignore(MARIONETTE)
  public void testShouldBeAbleToClickElementInAFrameThatIsOutOfView() {
    driver.get(appServer.whereIs("scrolling_tests/page_with_frame_out_of_view.html"));
    driver.switchTo().frame("frame");
    WebElement element = driver.findElement(By.name("checkbox"));
    element.click();
    assertTrue(element.isSelected());
  }

  @SwitchToTopAfterTest
  @Test
  @Ignore(SAFARI)
  public void testShouldBeAbleToClickElementThatIsOutOfViewInAFrame() {
    driver.get(appServer.whereIs("scrolling_tests/page_with_scrolling_frame.html"));
    driver.switchTo().frame("scrolling_frame");
    WebElement element = driver.findElement(By.name("scroll_checkbox"));
    element.click();
    assertTrue(element.isSelected());
  }

  @SwitchToTopAfterTest
  @Test
  @Ignore(value = ALL, reason = "All tested browses scroll non-scrollable frames")
  public void testShouldNotBeAbleToClickElementThatIsOutOfViewInANonScrollableFrame() {
    driver.get(appServer.whereIs("scrolling_tests/page_with_non_scrolling_frame.html"));
    driver.switchTo().frame("scrolling_frame");
    WebElement element = driver.findElement(By.name("scroll_checkbox"));
    Throwable t = catchThrowable(element::click);
    assertThat(t, instanceOf(MoveTargetOutOfBoundsException.class));
  }

  @SwitchToTopAfterTest
  @Test
  @Ignore(SAFARI)
  public void testShouldBeAbleToClickElementThatIsOutOfViewInAFrameThatIsOutOfView() {
    driver.get(appServer.whereIs("scrolling_tests/page_with_scrolling_frame_out_of_view.html"));
    driver.switchTo().frame("scrolling_frame");
    WebElement element = driver.findElement(By.name("scroll_checkbox"));
    element.click();
    assertTrue(element.isSelected());
  }

  @SwitchToTopAfterTest
  @Test
  @Ignore(SAFARI)
  public void testShouldBeAbleToClickElementThatIsOutOfViewInANestedFrame() {
    driver.get(appServer.whereIs("scrolling_tests/page_with_nested_scrolling_frames.html"));
    driver.switchTo().frame("scrolling_frame");
    driver.switchTo().frame("nested_scrolling_frame");
    WebElement element = driver.findElement(By.name("scroll_checkbox"));
    element.click();
    onlyPassIfNotOnMac(651, () -> assertTrue(element.isSelected()));
  }

  @SwitchToTopAfterTest
  @Test
  @Ignore(SAFARI)
  public void testShouldBeAbleToClickElementThatIsOutOfViewInANestedFrameThatIsOutOfView() {
    driver.get(appServer.whereIs("scrolling_tests/page_with_nested_scrolling_frames_out_of_view.html"));
    driver.switchTo().frame("scrolling_frame");
    driver.switchTo().frame("nested_scrolling_frame");
    WebElement element = driver.findElement(By.name("scroll_checkbox"));
    element.click();

    onlyPassIfNotOnMac(651, () -> assertTrue(element.isSelected()));
  }

  private void onlyPassIfNotOnMac(int mozIssue, Runnable toCheck) {
    try {
      toCheck.run();
      assumeFalse(
          "It appears https://github.com/mozilla/geckodriver/issues/" + mozIssue + " is fixed",
          Platform.getCurrent() == Platform.MAC && Browser.detect() == Browser.ff);
    } catch (Throwable e) {
      // Swallow the exception, as this is expected for Firefox on OS X
      if (!(Platform.getCurrent() == Platform.MAC && Browser.detect() == Browser.ff)) {
        throw e;
      }
    }
  }

  @Test
  public void testShouldNotScrollWhenGettingElementSize() {
    driver.get(appServer.whereIs("scroll3.html"));
    long scrollTop = getScrollTop();
    driver.findElement(By.id("button1")).getSize();
    assertEquals(scrollTop, getScrollTop());
  }

  private long getScrollTop() {
    wait.until(presenceOfElementLocated(By.tagName("body")));
    return (Long)((JavascriptExecutor)driver).executeScript("return document.body.scrollTop;");
  }

  @SwitchToTopAfterTest
  @Test
  @Ignore(SAFARI)
  @Ignore(MARIONETTE)
  public void testShouldBeAbleToClickElementInATallFrame() {
    driver.get(appServer.whereIs("scrolling_tests/page_with_tall_frame.html"));
    driver.switchTo().frame("tall_frame");
    WebElement element = driver.findElement(By.name("checkbox"));
    element.click();
    assertTrue(element.isSelected());
  }
}
