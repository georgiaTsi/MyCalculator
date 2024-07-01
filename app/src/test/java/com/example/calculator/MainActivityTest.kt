import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.calculator.MainActivity
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.example.calculator.R
import junit.framework.TestCase.assertEquals
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.`is`
import java.text.DecimalFormat

@RunWith(AndroidJUnit4::class)
class MainActivityEspressoTest {

    @Before
    fun launchActivity() {
        ActivityScenario.launch(MainActivity::class.java)
    }

    @Test
    fun clearButton_ClearsState() {
        Espresso.onView(withId(R.id.btn_5)).perform(click())
        Espresso.onView(withId(R.id.btn_minus)).perform(click())
        Espresso.onView(withId(R.id.btn_3)).perform(click())
        Espresso.onView(withId(R.id.btn_clear)).perform(click())

        Espresso.onView(withId(R.id.result)).check(matches(withText("")))
    }

    @Test
    fun formatNumber_formatsCorrectly() {
        val mainActivity = MainActivity()

        assertEquals("123.456", mainActivity.formatNumber(123.456))
        assertEquals("123", mainActivity.formatNumber(123.0))
        assertEquals("0", mainActivity.formatNumber(0.0))
        assertEquals("", mainActivity.formatNumber(null))
    }

    @Test
    fun performOperation_calculatesCorrectly() {
        val mainActivity = MainActivity()
        mainActivity.operand = 10.0
        mainActivity.performOperation(5.0, "+")
        assertEquals("15", mainActivity.resultTextView.text)

        mainActivity.operand = 10.0
        mainActivity.performOperation(5.0, "-")
        assertEquals("5", mainActivity.resultTextView.text)

        mainActivity.operand = 10.0
        mainActivity.performOperation(5.0, "×")
        assertEquals("50", mainActivity.resultTextView.text)

        mainActivity.operand = 10.0
        mainActivity.performOperation(5.0, "÷")
        assertEquals("2", mainActivity.resultTextView.text)

        mainActivity.operand = 100.0
        mainActivity.performOperation(5.0, "%")
        assertEquals("1", mainActivity.resultTextView.text)

        mainActivity.operand = 10.0
        mainActivity.performOperation(2.0, "^")
        assertEquals("100", mainActivity.resultTextView.text)

        mainActivity.operand = 100.0
        mainActivity.pendingOperation = "√"
        mainActivity.performOperation(0.0, "")
        assertEquals("10", mainActivity.resultTextView.text)
    }
}
