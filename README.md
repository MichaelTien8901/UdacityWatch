# UdacityWatch
   This is the android wear watchface project with companion program Sunshine from udacity.
   
## OpenWeatherMap API key definition.

openweathermap.org API key is defined in the file "security_string.xml" under the folder app/src/main/res/values.  
The API key can't be shared in public.  Please create the file with the following file template.
File "mobile/src/main/res/values/security_string.xml".  Using "RAW" format to reveal the following file content. 

<?xml version="1.0" encoding="utf-8"?>
<resources>
<string name="open_weather_api_key" translatable="false">[KEY DEFINE HERE]</string>
</resources>

## Synchronization of weather condition

   When watchface starting up, it will send message to Sunshine program to request synchronize the weather condition.  
When new weather data synchronized from OpenWeatherMap, Sunshine program will sync to wear watch.
   
## Settings   

   When Sunshine program change settings of location or temperature unit, the settings will sync to android wear 
automatically.


   
