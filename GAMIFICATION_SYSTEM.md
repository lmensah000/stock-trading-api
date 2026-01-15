# Innate Fitness - Advanced Gamification System

## Overview
Complete gamification system with levels, badges, avatars, referrals, and competitive group rankings.

---

## 🎮 LEVEL SYSTEM

### How It Works
- Users earn **XP (Experience Points)** from all point-earning activities
- XP accumulates to unlock new levels
- Each level has a unique name and avatar stage
- Level ups award bonus points

### 8 Levels
1. **Seedling** (0 XP) - Avatar Stage 1
2. **Sprout** (100 XP) - Avatar Stage 2 - +50 bonus points
3. **Budding** (250 XP) - Avatar Stage 3 - +75 bonus points
4. **Blooming** (500 XP) - Avatar Stage 4 - +100 bonus points
5. **Flourishing** (1000 XP) - Avatar Stage 5 - +150 bonus points
6. **Thriving** (2000 XP) - Avatar Stage 6 - +200 bonus points
7. **Radiant** (3500 XP) - Avatar Stage 7 - +300 bonus points
8. **Transcendent** (5000 XP) - Avatar Stage 8 - +500 bonus points

### API Endpoints
- `GET /api/levels` - Get all level information
- `GET /api/my/progress` - Get user's current level, XP, and progress to next level

---

## 🏆 BADGE SYSTEM

### Available Badges

| Badge ID | Name | Description | Requirement | Points |
|----------|------|-------------|-------------|--------|
| `first_workout` | First Steps | Completed first workout | 1 workout | 25 |
| `workout_warrior` | Workout Warrior | Completed 10 workouts | 10 workouts | 100 |
| `fitness_master` | Fitness Master | Completed 50 workouts | 50 workouts | 500 |
| `goal_crusher` | Goal Crusher | Completed 5 goals | 5 goals | 200 |
| `meal_planner` | Meal Planner | Generated 10 meal plans | 10 meals | 150 |
| `social_butterfly` | Social Butterfly | Shared 5 achievements | 5 shares | 100 |
| `referral_champion` | Referral Champion | Referred 5 friends | 5 referrals | 300 |
| `consistency_king` | Consistency King | 7-day workout streak | 7 days | 250 |
| `team_player` | Team Player | Won group challenge | 1st place | 200 |

### How Badges Work
- Automatically awarded when requirement is met
- Awards bonus points immediately
- Creates achievement notification
- Displayed on user profile
- Tracked in user's badge collection

### API Endpoints
- `GET /api/badges` - Get all badge information
- Badges are returned in `/api/my/progress`

---

## 👤 AVATAR SYSTEM

### How It Works
- Avatar evolves through 8 stages as user levels up
- Visual representation of user's progress
- Each level unlocks a new avatar stage

### Avatar Stages
- Stage 1: Seedling (small sprout)
- Stage 2: Sprout (growing plant)
- Stage 3: Budding (small leaves)
- Stage 4: Blooming (flowers appearing)
- Stage 5: Flourishing (full blooms)
- Stage 6: Thriving (vibrant growth)
- Stage 7: Radiant (glowing presence)
- Stage 8: Transcendent (ultimate form)

### Implementation
- `avatar_stage` field in user model
- Frontend displays corresponding avatar image/icon based on stage
- Updated automatically on level up

---

## 🎯 ONBOARDING COMPLETION

### Points Awarded
- **100 points** for completing onboarding

### How to Complete
User marks onboarding as complete when ready

### API Endpoint
```
POST /api/onboarding/complete
```

### Response
```json
{
  "message": "Onboarding completed!",
  "points_earned": 100
}
```

---

## 🤝 REFERRAL SYSTEM

### How It Works

#### Step 1: Referrer Shares Code
- Every user gets a unique referral code (e.g., "AB12CD34")
- Code visible in profile: `user.referral_code`
- Referrer shares code with friends

#### Step 2: New User Applies Code
- New user enters referral code during/after registration
- System validates code and creates referral record

#### Step 3: Referral Onboarding
New user must complete:
- ✅ 1 workout logged
- ✅ 1 meal plan generated
- ✅ 1 goal created

#### Step 4: Points Awarded
- **Referrer gets 200 points** when friend completes onboarding
- **Referee gets 50 points** for completing onboarding
- Badge "Referral Champion" awarded at 5 successful referrals

### API Endpoints

**Apply Referral Code**
```
POST /api/referrals/create
Body: { "referral_code": "AB12CD34" }
```

**Get My Referrals**
```
GET /api/referrals/my
Response: {
  "referral_code": "AB12CD34",
  "total_referrals": 3,
  "onboarded_referrals": 2,
  "referrals": [...]
}
```

**Check Onboarding Status**
```
POST /api/referrals/check-onboarding
```

---

## 🏅 GROUP GOAL RANKINGS

### How It Works

#### During Challenge
1. Members contribute to group goal
2. System tracks each member's contribution via points transactions
3. Real-time rankings available

#### Upon Completion
When group reaches target:

1. System calculates rankings based on contributions
2. Awards points based on rank:
   - **1st Place: 300 points** (+ Team Player badge)
   - **2nd Place: 200 points**
   - **3rd Place: 150 points**
   - **Participation: 100 points**
3. **BONUS**: If ALL members contributed, everyone gets +100 bonus points

### API Endpoints

**Get Current Rankings**
```
GET /api/groups/{group_id}/rankings
Response: [
  {
    "user_id": "...",
    "user_name": "Sarah",
    "contribution": 120,
    "rank": 1
  },
  ...
]
```

**Complete Group Goal**
```
POST /api/groups/{group_id}/complete
Response: {
  "message": "Group goal completed!",
  "rankings": [...],
  "all_members_contributed": true
}
```

---

## 📊 POINT EARNING SUMMARY

| Activity | Points | XP |
|----------|--------|-----|
| Complete onboarding | 100 | 100 |
| Workout (10-50 min) | 10-50 | 10-50 |
| Complete goal | 50 | 50 |
| Group progress | 20 | 20 |
| Share achievement | 5 | 5 |
| Referral onboarding | 200 (referrer) | 200 |
| Referral complete | 50 (referee) | 50 |
| Level up | 50-500 | 0 |
| Badge earned | 25-500 | 0 |
| Group completion 1st | 300 | 300 |
| Group completion 2nd | 200 | 200 |
| Group completion 3rd | 150 | 150 |
| Group participation | 100 | 100 |
| All members bonus | +100 | +100 |

---

## 🔄 AUTOMATIC BADGE TRIGGERS

Badges are automatically checked and awarded when:

- **Workout logged** → Checks: First Steps, Workout Warrior, Fitness Master
- **Meal plan generated** → Checks: Meal Planner
- **Goal completed** → Checks: Goal Crusher
- **Achievement shared** → Checks: Social Butterfly
- **Referral onboarded** → Checks: Referral Champion
- **Group goal won** → Checks: Team Player

---

## 💡 FRONTEND INTEGRATION NOTES

### User Object Fields
```javascript
{
  id: string,
  email: string,
  name: string,
  points: number,
  level: number,
  xp: number,
  avatar_stage: number,
  badges: string[],
  onboarding_completed: boolean,
  referral_code: string,
  total_workouts: number
}
```

### Display Recommendations

**Profile Page**
- Show avatar based on `avatar_stage`
- Display level name and progress bar
- Grid of earned badges (colored) + locked badges (grayed)
- Referral code with share button
- XP progress to next level

**Dashboard**
- Quick stats: Level, Points, Badges earned
- Progress bar to next level
- Recent badges earned

**Referral Section**
- "Invite Friends" prominent button
- Display referral code
- Share link generator
- List of referred friends and status

**Group Goals**
- Live leaderboard showing rankings
- Contribution amounts
- Projected points for each rank
- "Complete Goal" button when target reached

---

## 🎨 AVATAR IMAGE MAPPING

Recommended avatar images by stage:
1. 🌱 Seedling
2. 🌿 Sprout  
3. 🪴 Budding
4. 🌸 Blooming
5. 🌺 Flourishing
6. 🌻 Thriving
7. ✨ Radiant
8. 🌟 Transcendent

---

## ⚠️ IMPORTANT NOTES

1. **XP = Points**: All point-earning activities also earn XP for leveling
2. **Automatic Triggers**: Badges are checked automatically, no manual action needed
3. **One-Time Awards**: Badges can only be earned once per user
4. **Referral Validation**: Referee must complete onboarding (1 workout + 1 meal + 1 goal)
5. **Group Completion**: Only works when group reaches target value
6. **Contributions Tracked**: Via points transactions with group name in description

---

## 🚀 NEXT STEPS FOR FRONTEND

1. Create Progress/Profile page showing:
   - Avatar display
   - Level progress bar
   - Badge collection grid
   - Referral section

2. Add referral flow:
   - Input for entering referral code
   - Share referral code modal
   - Referral status tracker

3. Add group leaderboard:
   - Real-time rankings
   - Contribution visualization
   - Complete goal button

4. Add level-up celebrations:
   - Modal/animation on level up
   - Badge earned notifications
   - Toast messages for milestones

5. Add onboarding checklist:
   - Track completion of first workout, meal, goal
   - Show referral onboarding progress if applicable
