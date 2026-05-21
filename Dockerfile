version: '3.8'

services:
  fraud-service:
    build: ./fraud-service
    ports:
      - "8081:8081"
    networks:
      - app-network

  payment-service:
    build: ./payment-service
    ports:
      - "8082:8082"
    networks:
      - app-network

  order-service:
    build: ./order-service
    ports:
      - "8080:8080"
    environment:
      - SERVICES_FRAUD=http://fraud-service:8081
      - SERVICES_PAYMENT=http://payment-service:8082
    depends_on:
      - fraud-service
      - payment-service
    networks:
      - app-network

networks:
  app-network:
    driver: bridge
