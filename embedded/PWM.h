/***********************************************************************************
 * @file PWM.c
 * @author Elenbaas, Lynn, Mulder
 * @brief This code contains functions to initialize 3 PWM signals. Two signals are
 *            at 10k Hz to drive the DC motors and one signal is at 50Hz to drive a
 *            positional servo. 
 * @date 2022-05-31
 **********************************************************************************/
#include "sl_pwm.h"
#include "sl_pwm_instances.h"
#include "sl_sleeptimer.h"

// This enum is passed to the drive_motors function to define what direction to drive
enum direction {
  forward,
  backward,
};

/***********************************************************************************
 * @brief This function initializes PWM signals for two DC motors and 1 positional
 *            servo. 
 **********************************************************************************/
void init_motors();

/***********************************************************************************
 * @brief This function changes the duty cycle for the positional servo PWM signal.
 * 
 * @param uint8_t   percent - Duty cycle in % to use (0-12)
 **********************************************************************************/
void drive_motors(uint8_t percent, enum direction dir);

/***********************************************************************************
 * @brief This function changes the duty cycle of the DC motor PWM signal.This 
 *            function will also update the motor direction using GPIO pins.
 * 
 * @param uint8_t percent - Duty cycle in percent (0-100)
 * @param enum direction dir - drives motor "forward" or "backward"
 **********************************************************************************/
void drive_servo(uint8_t percent);
