/***********************************************************************************
 * @file app.h
 * @author Elenbaas, Lynn, Mulder
 * @brief This code contains the main app init function which is called one
 *            time by default before entering the main app loop.
 * @date 2022-05-31
 **********************************************************************************/

#ifndef APP_H
#define APP_H

#include "PWM.h"

/***********************************************************************************
 * @brief This function calls the initialization functions for the DC motors and 
 *            positional servo. 
 **********************************************************************************/
void app_init(void);

/***********************************************************************************
 * @brief This function runs the main loop of the embedded application. Currently
 *            the app cycles through different positions a positional servo. 
 **********************************************************************************/
void app_process_action(void);

#endif  // APP_H
