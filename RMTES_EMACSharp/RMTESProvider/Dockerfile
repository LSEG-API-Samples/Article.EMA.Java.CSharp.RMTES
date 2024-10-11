ARG DOTNET_VERSION=8.0
ARG VARIANT=alpine3.20
# Builder .NET SDK Image
FROM mcr.microsoft.com/dotnet/sdk:${DOTNET_VERSION} AS builder
LABEL maintainer="Developer Relations"

# Create app directory
WORKDIR /App

# Copy projecf file
COPY *.csproj ./
# Restore as distinct layers
RUN dotnet restore
# Copy everything else
COPY . ./
# Build and publish a release
RUN dotnet publish --configuration Release -o out

# Runner .NET runtime Image
FROM --platform=linux/amd64 mcr.microsoft.com/dotnet/runtime:${DOTNET_VERSION}-${VARIANT} as release
# Create app directory
WORKDIR /App
COPY --from=builder /App/out .
COPY EmaConfig.xml .
ADD etc /App/etc
ENTRYPOINT ["dotnet","RMTESProvider.dll"]