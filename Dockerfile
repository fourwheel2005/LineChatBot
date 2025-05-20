FROM eclipse-temurin:17-jdk-alpine

# สร้าง Directory สำหรับแอป
WORKDIR /app

# Copy ไฟล์ .jar ที่ build แล้ว
COPY build/libs/linechatbot-0.0.1-SNAPSHOT.jar app.jar

# รันแอป
ENTRYPOINT ["java", "-jar", "app.jar"]
