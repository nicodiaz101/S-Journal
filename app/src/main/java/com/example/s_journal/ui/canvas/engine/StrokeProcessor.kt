package com.example.s_journal.ui.canvas.engine

import android.view.MotionEvent
import com.example.s_journal.ui.canvas.model.StrokePoint

class StrokeProcessor {

    /**
     * Extracts all historical and current points from a stylus MotionEvent.
     * MotionEvent on Samsung Galaxy Tab digitizers aggregates multiple sub-frame samples.
     */
    fun processHistoricalAndCurrentPoints(
        event: MotionEvent,
        pointerIndex: Int = 0
    ): List<StrokePoint> {
        val points = mutableListOf<StrokePoint>()
        val historySize = event.historySize

        for (i in 0 until historySize) {
            val historicalX = event.getHistoricalX(pointerIndex, i)
            val historicalY = event.getHistoricalY(pointerIndex, i)
            val historicalPressure = event.getHistoricalPressure(pointerIndex, i).coerceIn(0.05f, 1.0f)
            val historicalTime = event.getHistoricalEventTime(i)

            points.add(
                StrokePoint(
                    x = historicalX,
                    y = historicalY,
                    pressure = historicalPressure,
                    timestamp = historicalTime
                )
            )
        }

        val currentX = event.getX(pointerIndex)
        val currentY = event.getY(pointerIndex)
        val currentPressure = event.getPressure(pointerIndex).coerceIn(0.05f, 1.0f)
        val currentTime = event.eventTime

        points.add(
            StrokePoint(
                x = currentX,
                y = currentY,
                pressure = currentPressure,
                timestamp = currentTime
            )
        )

        return points
    }
}
