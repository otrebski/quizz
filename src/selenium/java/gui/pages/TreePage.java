package gui.pages;

import org.fluentlenium.core.FluentPage;
import org.fluentlenium.core.annotation.Page;
import org.fluentlenium.core.domain.FluentList;
import org.fluentlenium.core.domain.FluentWebElement;

import java.time.Duration;

import static org.fluentlenium.core.filter.FilterConstructor.*;

public class TreePage extends FluentPage {

    @Page
    Home home;

    private final Duration tenSeconds = Duration.ofSeconds(10);

    public TreePage select(String option) throws InterruptedException {
        Thread.sleep(3000);
        FluentList<FluentWebElement> question = $(withId("question")).
                $(withText(option));
        question.await().atMost(tenSeconds).until().clickable();
        question.click();
        return this;
    }

    public TreePage selectHistory(String option) throws InterruptedException {
        Thread.sleep(2000);
        $(withClass("historyStep")).$(withText(option)).click();
        return this;
    }

    public TreePage validateIsFinal(String solution) throws InterruptedException {
        Thread.sleep(2000);
        $(withId("final_step")).$(withText(solution)).await().atMost(tenSeconds).until().displayed();
        return this;
    }

    public TreePage sendFeedbackPositive(String feedback) {
        $(withId("feedback-text")).fill().withText(feedback);
        $(withId("feedback-rate+1")).click();
        $(withId("feedback-send-confirmation")).await().atMost(tenSeconds).until().displayed();
        return this;
    }

    public Home goHome(){
        return goTo(home);
    }

}
