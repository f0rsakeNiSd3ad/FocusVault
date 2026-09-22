# FocusVault

Welcome to **FocusVault**! This is a hardcore, gamified productivity tool designed to physically block your worst digital distractions (like social media websites, video games, and chat apps) and force you to earn back your leisure time. 

## The Motivation
The inspiration for this project comes from the original concept of **ReGrind**. I loved the idea of treating focus as a currency—having to genuinely "grind" through work or study sessions to earn the right to play video games or browse the web. FocusVault takes that brilliant concept and makes it a fully realized, locally hosted, and highly optimized reality for Windows.

## How It Works
FocusVault operates on a simple economy: **Grind to Play.**

1. **Focus Sessions**: When you start a Focus Session, your configured websites and applications are strictly blocked. For every minute you spend focusing, you earn 1 Credit (CR).
2. **Specialized Goals**: You can set specific goals (like "Study for 2 hours") to track your progress directly on the dashboard.
3. **Unlock Sessions**: Ready to take a break? Spend your hard-earned Credits to start an Unlock Session. For the duration of this session, FocusVault will temporarily unblock your restricted apps and websites. 
4. **Hardcore Blocking**: Unlike casual browser extensions, FocusVault uses native Windows APIs to instantly snipe specific browser tabs with blocked domains and entirely terminate blocked executable processes.

## Getting Started

### Prerequisites
- **Java 17** or higher
- **Maven**
- **Windows OS** (FocusVault utilizes native Windows APIs via JNA for optimal performance and zero-stutter blocking).

### Running the App
To run FocusVault properly and ensure it can enforce your blocks:
1. Open your terminal (Command Prompt, PowerShell, or your IDE terminal).
2. **CRITICAL:** You must run your terminal as an **Administrator**.
3. Navigate to the project directory.
4. Run the following command:
   ```bash
   mvn clean javafx:run
   ```

## Why Administrator Permissions?
FocusVault is a hardcore blocker designed to actually stop you from getting distracted. To do this, it requires elevated (Administrator) permissions for two main reasons:
- **Domain Blocking:** It modifies your Windows system `hosts` file (`C:\Windows\System32\drivers\etc\hosts`) to block websites at the network level. Windows fiercely protects this file.
- **Process Blocking:** It uses native Windows API calls to monitor and terminate background processes, which requires high-level system access.

## Privacy Disclaimer
**100% Offline. 100% Private.**
FocusVault is completely local to your machine. It requires **no internet connection**, has **no servers**, and **collects absolutely zero data**. Your blocked lists, credits, and habits stay entirely in your own computer's Windows Registry and are never transmitted anywhere.
