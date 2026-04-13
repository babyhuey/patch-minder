FROM thyrlian/android-sdk:latest

WORKDIR /project

# Install required SDK components and accept licenses
RUN yes | sdkmanager --licenses > /dev/null 2>&1 || true
RUN sdkmanager "platforms;android-35" "build-tools;35.0.0"

# Project is mounted at runtime, not copied
CMD ["./gradlew", "assembleDebug"]
