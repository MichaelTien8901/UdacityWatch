# UdacityWatch

## Overview

UdacityWatch is the android wear(1.0) watchface project with companion program Sunshine from Udacity.  

Sunshine is the weather application from Udacity.

## Pre-requisites

* Android SDK v23
* Android Build Tools v23.0.1
* Android Support Repository
* Google Play Services

## OpenWeatherMap API key definition

openweathermap.org API key is defined in the file "security_string.xml" under the folder app/src/main/res/values.  
The API key can't be shared in public.  Please create the file with the following file template.
File "mobile/src/main/res/values/security_string.xml".  Using "RAW" format to reveal the following file content. 

<?xml version="1.0" encoding="utf-8"?>
<resources>
<string name="open_weather_api_key" translatable="false">[KEY DEFINE HERE]</string>
</resources>

## Synchronization of weather condition

<<<<<<< HEAD
When watchface starting up, it will send message to Sunshine program to request synchronization of weather condition.  
When new weather data synchronzied from OpenWeatherMap, Sunshine program will sync to wear watch.
=======
   When watchface starting up, it will send message to Sunshine program to request synchronize the weather condition.  
When new weather data synchronized from OpenWeatherMap, Sunshine program will sync to wear watch.
>>>>>>> 0a3b0e849dc5361babac992dea28ab00a1971991
   
## Settings   

When Sunshine program change settings of location or temperature unit, the settings will sync to android wear.


   
