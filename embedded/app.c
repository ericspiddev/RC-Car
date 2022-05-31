/***********************************************************************************
 * @file app.c
 * @author Elenbaas, Lynn, Mulder
 * @brief This code contains the main app init function which is called one
 *            time by default before entering the main app loop.
 * @date 2022-05-31
 **********************************************************************************/
#include "app.h"

// Stores either forward or backward. used to drive the DC motors
enum direction dir = forward;

/***********************************************************************************
 * @brief This function calls the initialization functions for the DC motors and 
 *            positional servo. 
 **********************************************************************************/
void app_init(void) {
  init_motors();
  drive_motors(50, dir);
}

/***********************************************************************************
 * @brief This function runs the main loop of the embedded application. Currently
 *            the app cycles through different positions a positional servo. 
 **********************************************************************************/
void app_process_action(void) {
  for (int i = 0; i < 12; i++) {
      drive_servo(i);
      sl_sleeptimer_delay_millisecond(100);
  }
  for (int i = 12; i > 0; i--) {
      drive_servo(i);
      sl_sleeptimer_delay_millisecond(100);
  }
}
