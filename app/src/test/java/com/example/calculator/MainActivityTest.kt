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
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.`is`

@RunWith(AndroidJUnit4::class)
class MainActivityEspressoTest {

    @Before
    fun launchActivity() {
        ActivityScenario.launch(MainActivity::class.java)
    }

    @Test
    fun typeNumberAndPerformOperation_Addition() {
        // Type numbers and perform addition operation
        Espresso.onView(withId(R.id.btn_1)).perform(click())
        Espresso.onView(withId(R.id.btn_plus)).perform(click())
        Espresso.onView(withId(R.id.btn_2)).perform(click())
        Espresso.onView(withId(R.id.btn_equals)).perform(click())

        // Verify the result is displayed correctly
        Espresso.onView(withId(R.id.result)).check(matches(withText("3")))
    }

    @Test
    fun clearButton_ClearsState() {
        // Perform a series of operations and then clear
        Espresso.onView(withId(R.id.btn_5)).perform(click())
        Espresso.onView(withId(R.id.btn_minus)).perform(click())
        Espresso.onView(withId(R.id.btn_3)).perform(click())
        Espresso.onView(withId(R.id.btn_clear)).perform(click())

        // Verify the result is cleared
        Espresso.onView(withId(R.id.result)).check(matches(withText("")))
    }

    @Test
    fun convertCurrency_Success() {
        // Simulate selecting currency conversion and verifying result
        Espresso.onView(withId(R.id.spinner_from)).perform(click())
        Espresso.onData(allOf(`is`(instanceOf(String::class.java)), `is`("USD"))).perform(click())

        Espresso.onView(withId(R.id.spinner_to)).perform(click())
        Espresso.onData(allOf(`is`(instanceOf(String::class.java)), `is`("EUR"))).perform(click())

        Espresso.onView(withId(R.id.button_calculate)).perform(click())

        // Verify the conversion result is displayed correctly
        Espresso.onView(withId(R.id.textview_calculate_result)).check(matches(withText("8.6")))
    }
}
