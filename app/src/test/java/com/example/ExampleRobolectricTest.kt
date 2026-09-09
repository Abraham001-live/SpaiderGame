package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.engine.Vec3
import com.example.game.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Spider Hunt", appName)
  }

  @Test
  fun `test spider precision eating mechanics`() {
    val spider = SpiderEntity(position = Vec3(0f, 0.4f, 0f), yawDeg = 0f)
    val prey = PreyEntity(id = 1, type = PreyType.JUNGLE_RODENT, position = Vec3(0f, 0.35f, 1.5f), yawDeg = 180f)

    val target = spider.findTargetToEat(listOf(prey))
    assertNotNull("Should find prey directly in front within bite range", target)

    val initialFood = spider.food
    spider.performEat(prey)
    assertTrue("Prey should be consumed", !prey.isAlive)
    assertTrue("Spider food should increase", spider.food > initialFood)
  }

  @Test
  fun `test frog web stun mechanics`() {
    val frog = FrogEntity(position = Vec3(0f, 0.5f, 10f), yawDeg = 180f)
    frog.state = FrogState.CHASE
    frog.onHitByWeb(5.0f)

    assertEquals("Frog should be stunned when web hits", FrogState.STUNNED, frog.state)
    assertEquals(5.0f, frog.stunTimer, 0.01f)
  }
}

