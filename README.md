
# Context Monitoring App

## Abstract
This project involves developing an Android application that monitors vital signs such as heart rate and breathing rate using built-in Android sensors. The app measures:
- **Heart rate** using the phone's rear camera and flash.
- **Breathing rate** using the accelerometer.
- **Symptom data** stored in a local database.

The integration of these features aims to offer users a convenient and non-invasive method to track their vital signs, potentially aiding in early detection of health issues. This paper discusses the technical approach, design choices, implications & limitations, and the potential impact of this application on personal health management.

## Introduction
The rapid advancement of mobile technology has paved the way for innovative applications that leverage smartphone sensors to monitor and improve health outcomes. This app aims to:
- Capture vital signs such as **heart rate** and **respiratory rate**.
- Collect **symptom data** for personalized health tracking.

### Data Collection Methods
- **Heart Rate:** Recorded by capturing a 45-second video of the user's index finger placed on the smartphone's camera lens with the flash enabled. The variation in red coloration in the video is used to derive the heart rate.
- **Respiratory Rate:** Measured by having the user lie down with the smartphone on their chest for 45 seconds. The accelerometer or orientation sensor data is used to compute the respiratory rate.
- **Symptom Tracking:** Users can log symptoms from a predefined list and rate them on a scale of 1 to 5. All data is stored in a local database using RoomDB or RealmDB.

## Technical Approach
### Heart Rate Measurement
The `captureVideo` method records a video and calculates the heart rate using:
- Device camera with flash enabled.
- `videoCapture` for recording.
- `heartRateCalculator` function processes video to determine heart rate.

### Respiratory Rate Measurement
The respiratory rate is calculated by collecting **accelerometer data** for 45 seconds:
- X, Y, and Z axis values are accumulated.
- Timer stops at 45 seconds.
- `respiratoryRateCalculator` processes data to calculate respiratory rate.

### Symptoms Data Storage
- **Heart Rate & Respiratory Rate** are imported from `MainActivity`.
- **10 user-defined symptoms** stored in a table in RoomDB.

## Design Choices
### Home Page
Designed for ease of use, allowing users to:
- Record **heart rate** and **respiratory rate**.
- Hide "Next" button during measurement.
- Automatically process and store health data.

### Symptoms Page
Users can:
- Scroll and mark symptoms with severity levels (0-5).
- Submit data to the database.
- Clear symptoms if needed.

## Implications & Limitations
### Implications
1. **Health Monitoring:** Convenient tracking of vital signs.
2. **Data-Driven Insights:** Useful for users and healthcare providers.
3. **Accessibility:** No need for specialized medical equipment.
4. **Research:** Enables study of health trends.

### Limitations
1. **Accuracy:** Measurements may be affected by movement or lighting.
2. **User Compliance:** Requires correct usage for accurate results.
3. **Data Privacy:** Sensitive health data must be securely stored.
4. **Limited Scope:** Only tracks select health parameters.

## Suggestions for Future Work
- Implement **machine learning** for risk prediction.
- **Cloud storage** for scalability and accessibility.
- Expand **symptom tracking capabilities**.
- Improve **data privacy and security**.

## Links
- **GitHub Repository:** [PipKcK/Context-Monitoring-App-535](https://github.com/PipKcK/Context-Monitoring-App-535)
- **YouTube Demo:** [Context Monitoring App](https://youtu.be/ajHBjoUeZhU)

## References
1. Milazzo, J., Bagade, P., Banerjee, A., & Gupta, S. K. S. (2013). *bHealthy: A physiological feedback-based mobile wellness application suite*. ACM.
2. Banerjee, A., Verma, S., Bagade, P., & Gupta, S. K. S. (2012). *Health-Dev: Model based development of pervasive health monitoring systems*. IEEE.



## Additional Questions

1. To use the Health-Dev framework for developing my context-sensing application, I'd need to provide a few key specifications. First, I'd outline the Sensor Specifications, detailing which sensors I’ll use, like an ECG sensor for heart rate monitoring, the type of data they’ll collect, and the sampling frequency. Next, in the Network Specifications outline, I'd describe how these sensors would communicate, the communication protocol (like Bluetooth), network topology, and any energy management details. For the Smartphone Specifications outline, I’ll define the user interface, such as buttons for starting data collection and graphs for displaying readings, and specify any algorithms for processing the data. Finally, in the Algorithm Specifications outline, I’ll list the algorithms required for analyzing the sensor data, like heart rate calculations, and whether I’ll use Health-Dev’s algorithms or my custom ones. By providing all these specifications, Health-Dev will generate the necessary code, making the development process easier without needing in-depth coding knowledge.

2. The bHealthy application suite provides a comprehensive solution for tracking and improving user well-being via physiological input. Here's how I can use it to develop a unique application: First, I'll employ physiological monitoring, which involves integrating sensors such as ECG, EEG, and accelerometers to collect real-time data on the user's mental and physical states. Next, I'll utilize evaluation and suggestion features to analyze the data, detecting the user's emotional state (for example, relaxation or frustration) and recommending activities or apps that enhance well-being. I can also create training applications, such as PETPeeves or BrainHealth, that involve users in health-promoting workouts or neurofeedback activities. Finally, I'll create wellness reports by aggregating data from multiple activities and apps to provide unique insights into the user's well-being and inform future recommendations. By combining these aspects, I can create an application that not only detects the user's context but also provides individualized feedback and recommendations to help them improve their health.

3. I would say that mobile computing encompasses much more than just app development. It involves gathering data from various sensors and analyzing it to provide meaningful insights; for instance, my project requires capturing heart rate and respiratory rate using smartphone sensors. Mobile applications also need to understand the user’s context to offer relevant information and services, which means recognizing the user’s environment, activities, and preferences. Effective mobile computing includes feedback mechanisms to inform users based on the collected data, enhancing their experience and engagement. Additionally, it often requires integration with other systems and databases, such as storing data in RoomDB or RealmDB, as highlighted in my project.
