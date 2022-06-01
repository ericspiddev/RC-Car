/***********************************************************************************
 * @file PWM.c
 * @author Elenbaas, Lynn, Mulder
 * @brief This code contains functions to initialize 3 PWM signals. Two signals are
 *            at 10k Hz to drive the DC motors and one signal is at 50Hz to drive a
 *            positional servo. 
 * @date 2022-05-31
 **********************************************************************************/
#include "PWM.h"

/***********************************************************************************
 * @brief This function initializes PWM signals for two DC motors and 1 positional
 *            servo. 
 **********************************************************************************/
void init_motors() {
  // Use simplicity studio's sl structs to initialize PWM signals
  sl_pwm_start(&sl_pwm_motor_l);
  sl_pwm_start(&sl_pwm_motor_r);
  sl_pwm_start(&sl_pwm_servo);
}

/***********************************************************************************
 * @brief This function changes the duty cycle for the positional servo PWM signal.
 * 
 * @param uint8_t   percent - Duty cycle in % to use (0-12)
 **********************************************************************************/
void drive_servo(uint8_t percent) {
    // Duty cycles from 0-12% are valid to send to the positional servo
    sl_pwm_set_duty_cycle(&sl_pwm_servo, DC);
}

/***********************************************************************************
 * @brief This function changes the duty cycle of the DC motor PWM signal.This 
 *            function will also update the motor direction using GPIO pins.
 * 
 * @param uint8_t percent - Duty cycle in percent (0-100)
 * @param enum direction dir - drives motor "forward" or "backward"
 **********************************************************************************/
void drive_motors(uint8_t percent, enum direction dir) {
  // TODO - What needs to happen here to control direction?
  //        additional GPIOs to h-bridge?
  if (dir == forward) {

  }
  else if (dir == backward) {

  }

  // Use sl_pwm_instance_t structs to set the duty cycle
  sl_pwm_set_duty_cycle(&sl_pwm_motor_l, percent);
  sl_pwm_set_duty_cycle(&sl_pwm_motor_r, percent);
}




