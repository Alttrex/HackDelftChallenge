package com.github.alttrex.hackdelftchallenge.listeners

import com.github.alttrex.hackdelftchallenge.achievements.Achievements
import com.github.alttrex.hackdelftchallenge.state.PetState
import com.intellij.execution.ExecutionListener
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.thisLogger
import java.util.Timer
import kotlin.concurrent.schedule

/**
 * Intercepts build/run executions to trigger the pet's "eating code → pooping build artifact" animation.
 * Registered via plugin.xml as a project-level message bus listener.
 * dsfdsfdsffdsfs
 * dfdsfxfxdfx
 * dxfxdfdxfx
 * dxffxfxd
 * fasfdsfds
 * dfdsfdsf
 *
 */
class BuildHook : ExecutionListener {

    override fun processStarting(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
        thisLogger().info("DevPet: Build/run started — pet is eating code!")
        PetState.getInstance().setEating()
    }

    override fun processTerminated(
        executorId: String,
        env: ExecutionEnvironment,
        handler: ProcessHandler,
        exitCode: Int
    ) {
        val success = exitCode == 0
        val state = PetState.getInstance()

        if (success) {
            thisLogger().info("DevPet: Build succeeded — pet poops out artifact! 💩✨")
            println("[DevPet] ✅ Build succeeded — artifact pooped out! +25 XP, +10 DevCoins")
            state.setBuilding()
            state.recordBuild(true)
        } else {
            thisLogger().info("DevPet: Build failed — pet is sick! 🤢")
            println("[DevPet] ❌ Build failed — pet is concussed/sick!")
            state.makeSick()
            state.recordBuild(false)
        }

        Achievements.check(env.project)

        // Reset mood after animation delay (3 seconds)
        Timer().schedule(3000L) {
            state.resetMoodFromAnimation()
        }
    }
}
