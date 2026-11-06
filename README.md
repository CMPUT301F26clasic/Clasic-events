# Clasic-events (clasiCampsite)

## Project Description
**Clasic-events** is an Android app for reserving popular campsites in Alberta through a fair, lottery-based system. Instead of refreshing a booking page the moment spots open, entrants join a campsite's waiting list during a registration period. When it closes, the app randomly draws winners, who can then choose their dates and reserve, or decline so the spot goes to someone else.

This gives people with busy schedules, disabilities, or other limitations the same chance at a booking as everyone else.

## Features
**Entrants**
- Search and filter campsites by location, dates, and number of guests
- View campsite details and join or leave a lottery waiting list
- Get notified when they win or lose a lottery
- Choose dates and reserve, or decline a win
- Manage their profile

**Campsite Owners**
- Register campsites with name, location, description, occupancy, price, and tags
- View and edit their campsites from a "My Campsites" list
- Create lotteries with registration and booking periods and a number of winners
- View the entrants on their waiting list

**Administrators**
- Browse and remove campsites and profiles that break app policy

## Team Members

| Name | GitHub Username | Main Area (Part 3) |
| :--- | :--- | :--- |
| Emery Theroux | [emery-collab](https://github.com/emery-collab) | Owner lotteries |
| Evan Rosenblood | [EvanRosenblood](https://github.com/EvanRosenblood) | Setup, profile, admin profiles |
| Keya Modi | [Keya_M](https://github.com/KeyaModi) | Lottery engine, waitlist, notifications |
| Dhavanil Patel | [dhavanilp](https://github.com/dhavanilp) | Finding campsites, admin campsites |
| Manush Patel | [Manush10](https://github.com/Manush10) | Booking and reservations |
| Steve Thomas | [SteveThomas3269](https://github.com/SteveThomas3269) | Owner campsites (register, view/edit, My Campsites) |

## Project Documentation
These documents cover the design, planning, and structure of Clasic-events.

### UI Mockups
The proposed screens and interface design, showing the intended look of the app.
* [View UI Mockups](https://github.com/CMPUT301F26clasic/Clasic-events/wiki/UI-Mockups)

### Storyboard
How users move through the app and interact with each screen and feature.
* [View Storyboard](https://github.com/CMPUT301F26clasic/Clasic-events/wiki/Storyboard)

### CRC Cards
Our main classes, with their responsibilities and collaborators. These guided our object-oriented design.
* [View CRC Cards](https://github.com/CMPUT301F26clasic/Clasic-events/wiki/CRC-Cards)

## Project Wiki
For design decisions, implementation details, and development notes, see our [GitHub Wiki](https://github.com/CMPUT301F26clasic/Clasic-events/wiki).

## AI Usage
Our team used AI tools to help with parts of this project. All AI-assisted code was reviewed, tested, and edited by the team member responsible for it. Files with AI help include a citation comment at the top, for example:

`// Parts of this file were written with help from Claude (Anthropic), 2026-11-03`

| Tool | How it was used |
| :--- | :--- |
| Claude (Anthropic) | Help writing Compose screens, view models, and tests; resolving Git merge conflicts; explaining Firestore usage |
| ChatGPT (OpenAI) | Suggested UI and function layouts for some screens (e.g. Register Campsite screen) |
| GitHub Copilot | Code autocompletion in Android Studio |
| Gemini (Google) | Debugging build and Gradle errors |
