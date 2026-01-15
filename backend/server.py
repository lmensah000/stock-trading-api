from fastapi import FastAPI, APIRouter, HTTPException, Depends, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from dotenv import load_dotenv
from starlette.middleware.cors import CORSMiddleware
from motor.motor_asyncio import AsyncIOMotorClient
import os
import logging
from pathlib import Path
from pydantic import BaseModel, Field, ConfigDict, EmailStr
from typing import List, Optional
import uuid
from datetime import datetime, timezone, timedelta
from passlib.context import CryptContext
from jose import JWTError, jwt
from emergentintegrations.llm.chat import LlmChat, UserMessage

ROOT_DIR = Path(__file__).parent
load_dotenv(ROOT_DIR / '.env')

# MongoDB connection
mongo_url = os.environ['MONGO_URL']
client = AsyncIOMotorClient(mongo_url)
db = client[os.environ['DB_NAME']]

# Security
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
security = HTTPBearer()

JWT_SECRET = os.environ.get('JWT_SECRET_KEY')
JWT_ALGORITHM = os.environ.get('JWT_ALGORITHM', 'HS256')
JWT_EXPIRATION = int(os.environ.get('JWT_EXPIRATION_MINUTES', 43200))

# Create the main app without a prefix
app = FastAPI()

# Create a router with the /api prefix
api_router = APIRouter(prefix="/api")

# ============= MODELS =============

class User(BaseModel):
    model_config = ConfigDict(extra="ignore")
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    email: EmailStr
    name: str
    password_hash: str
    points: int = 0
    total_workouts: int = 0
    level: int = 1
    xp: int = 0
    avatar_stage: int = 1
    badges: List[str] = []
    onboarding_completed: bool = False
    referral_code: str = Field(default_factory=lambda: str(uuid.uuid4())[:8].upper())
    referred_by: Optional[str] = None
    created_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))

class UserCreate(BaseModel):
    email: EmailStr
    name: str
    password: str

class UserLogin(BaseModel):
    email: EmailStr
    password: str

class UserResponse(BaseModel):
    id: str
    email: str
    name: str
    points: int
    total_workouts: int
    level: int
    xp: int
    avatar_stage: int
    badges: List[str]
    onboarding_completed: bool
    referral_code: str
    created_at: datetime

class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: UserResponse

class GroceryItem(BaseModel):
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    name: str
    quantity: str
    unit: str

class MealPlan(BaseModel):
    model_config = ConfigDict(extra="ignore")
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    user_id: str
    meal_name: str
    ingredients: List[str]
    instructions: str
    calories: Optional[int] = None
    protein: Optional[int] = None
    created_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))

class MealPlanRequest(BaseModel):
    groceries: List[GroceryItem]
    preferences: Optional[str] = None

class Goal(BaseModel):
    model_config = ConfigDict(extra="ignore")
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    user_id: str
    title: str
    description: str
    target_value: float
    current_value: float = 0.0
    unit: str
    deadline: Optional[datetime] = None
    status: str = "active"
    created_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))

class GoalCreate(BaseModel):
    title: str
    description: str
    target_value: float
    unit: str
    deadline: Optional[datetime] = None

class GoalUpdate(BaseModel):
    current_value: Optional[float] = None
    status: Optional[str] = None

class GroupGoal(BaseModel):
    model_config = ConfigDict(extra="ignore")
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    name: str
    description: str
    members: List[str]
    target_value: float
    current_value: float = 0.0
    unit: str
    created_by: str
    created_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))

class GroupGoalCreate(BaseModel):
    name: str
    description: str
    target_value: float
    unit: str

class Message(BaseModel):
    model_config = ConfigDict(extra="ignore")
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    group_goal_id: str
    user_id: str
    user_name: str
    content: str
    created_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))

class MessageCreate(BaseModel):
    content: str

class WeightEntry(BaseModel):
    model_config = ConfigDict(extra="ignore")
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    user_id: str
    weight: float
    unit: str = "kg"
    source: str = "manual"
    notes: Optional[str] = None
    created_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))

class WeightEntryCreate(BaseModel):
    weight: float
    unit: str = "kg"
    notes: Optional[str] = None

class FitnessData(BaseModel):
    model_config = ConfigDict(extra="ignore")
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    user_id: str
    source: str
    steps: Optional[int] = None
    calories_burned: Optional[int] = None
    distance: Optional[float] = None
    active_minutes: Optional[int] = None
    date: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))

# NEW MODELS FOR ENHANCED FEATURES

class WorkoutVideo(BaseModel):
    model_config = ConfigDict(extra="ignore")
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    title: str
    description: str
    category: str
    difficulty: str
    duration_minutes: int
    video_url: str
    thumbnail_url: str
    points_reward: int = 10

class WorkoutLog(BaseModel):
    model_config = ConfigDict(extra="ignore")
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    user_id: str
    workout_video_id: Optional[str] = None
    workout_type: str
    duration_minutes: int
    calories_burned: Optional[int] = None
    notes: Optional[str] = None
    points_earned: int = 10
    created_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))

class WorkoutLogCreate(BaseModel):
    workout_video_id: Optional[str] = None
    workout_type: str
    duration_minutes: int
    calories_burned: Optional[int] = None
    notes: Optional[str] = None

class PointsTransaction(BaseModel):
    model_config = ConfigDict(extra="ignore")
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    user_id: str
    points: int
    transaction_type: str
    description: str
    created_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))

class ShopItem(BaseModel):
    model_config = ConfigDict(extra="ignore")
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    name: str
    description: str
    category: str
    price_points: int
    image_url: str
    stock: int = 100

class RewardPurchase(BaseModel):
    model_config = ConfigDict(extra="ignore")
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    user_id: str
    shop_item_id: str
    item_name: str
    points_spent: int
    status: str = "pending"
    created_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))

class RewardPurchaseCreate(BaseModel):
    shop_item_id: str

class Achievement(BaseModel):
    model_config = ConfigDict(extra="ignore")
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    user_id: str
    title: str
    description: str
    type: str
    icon: str
    shared: bool = False
    created_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))

class ShareAchievement(BaseModel):
    achievement_id: str
    platform: str

# NEW GAMIFICATION MODELS

class Badge(BaseModel):
    model_config = ConfigDict(extra="ignore")
    id: str
    name: str
    description: str
    icon: str
    points_reward: int
    requirement: str

class Level(BaseModel):
    level: int
    name: str
    xp_required: int
    avatar_stage: int
    points_reward: int

class Referral(BaseModel):
    model_config = ConfigDict(extra="ignore")
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    referrer_id: str
    referee_id: str
    referee_email: str
    status: str = "pending"
    onboarded: bool = False
    created_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))

class ReferralCreate(BaseModel):
    referral_code: str

class GroupGoalRanking(BaseModel):
    user_id: str
    user_name: str
    contribution: float
    rank: int
    points_earned: int

# ============= AUTH UTILITIES =============

def hash_password(password: str) -> str:
    return pwd_context.hash(password)

def verify_password(plain_password: str, hashed_password: str) -> bool:
    return pwd_context.verify(plain_password, hashed_password)

def create_access_token(data: dict) -> str:
    to_encode = data.copy()
    expire = datetime.now(timezone.utc) + timedelta(minutes=JWT_EXPIRATION)
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, JWT_SECRET, algorithm=JWT_ALGORITHM)

async def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(security)) -> dict:
    try:
        token = credentials.credentials
        payload = jwt.decode(token, JWT_SECRET, algorithms=[JWT_ALGORITHM])
        user_id: str = payload.get("sub")
        if user_id is None:
            raise HTTPException(status_code=401, detail="Invalid authentication")
        
        user = await db.users.find_one({"id": user_id}, {"_id": 0})
        if user is None:
            raise HTTPException(status_code=401, detail="User not found")
        return user
    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid authentication")

async def award_points(user_id: str, points: int, transaction_type: str, description: str):
    """Helper function to award points to user"""
    await db.users.update_one({"id": user_id}, {"$inc": {"points": points, "xp": points}})
    
    transaction = PointsTransaction(
        user_id=user_id,
        points=points,
        transaction_type=transaction_type,
        description=description
    )
    doc = transaction.model_dump()
    doc['created_at'] = doc['created_at'].isoformat()
    await db.points_transactions.insert_one(doc)
    
    # Check for level up
    await check_level_up(user_id)

# Level progression system
LEVEL_SYSTEM = [
    {"level": 1, "name": "Seedling", "xp_required": 0, "avatar_stage": 1, "points_reward": 0},
    {"level": 2, "name": "Sprout", "xp_required": 100, "avatar_stage": 2, "points_reward": 50},
    {"level": 3, "name": "Budding", "xp_required": 250, "avatar_stage": 3, "points_reward": 75},
    {"level": 4, "name": "Blooming", "xp_required": 500, "avatar_stage": 4, "points_reward": 100},
    {"level": 5, "name": "Flourishing", "xp_required": 1000, "avatar_stage": 5, "points_reward": 150},
    {"level": 6, "name": "Thriving", "xp_required": 2000, "avatar_stage": 6, "points_reward": 200},
    {"level": 7, "name": "Radiant", "xp_required": 3500, "avatar_stage": 7, "points_reward": 300},
    {"level": 8, "name": "Transcendent", "xp_required": 5000, "avatar_stage": 8, "points_reward": 500},
]

BADGE_SYSTEM = [
    {"id": "first_workout", "name": "First Steps", "description": "Completed your first workout", "icon": "🏃", "points_reward": 25, "requirement": "1_workout"},
    {"id": "workout_warrior", "name": "Workout Warrior", "description": "Completed 10 workouts", "icon": "💪", "points_reward": 100, "requirement": "10_workouts"},
    {"id": "fitness_master", "name": "Fitness Master", "description": "Completed 50 workouts", "icon": "🏆", "points_reward": 500, "requirement": "50_workouts"},
    {"id": "goal_crusher", "name": "Goal Crusher", "description": "Completed 5 goals", "icon": "🎯", "points_reward": 200, "requirement": "5_goals"},
    {"id": "meal_planner", "name": "Meal Planner", "description": "Generated 10 meal plans", "icon": "🍽️", "points_reward": 150, "requirement": "10_meals"},
    {"id": "social_butterfly", "name": "Social Butterfly", "description": "Shared 5 achievements", "icon": "🦋", "points_reward": 100, "requirement": "5_shares"},
    {"id": "referral_champion", "name": "Referral Champion", "description": "Referred 5 friends", "icon": "🌟", "points_reward": 300, "requirement": "5_referrals"},
    {"id": "consistency_king", "name": "Consistency King", "description": "7-day workout streak", "icon": "👑", "points_reward": 250, "requirement": "7_day_streak"},
    {"id": "team_player", "name": "Team Player", "description": "Won a group challenge", "icon": "🤝", "points_reward": 200, "requirement": "group_win"},
]

async def check_level_up(user_id: str):
    """Check if user leveled up and award bonus"""
    user = await db.users.find_one({"id": user_id}, {"_id": 0})
    current_xp = user.get('xp', 0)
    current_level = user.get('level', 1)
    
    # Find next level
    for level_data in LEVEL_SYSTEM:
        if current_xp >= level_data['xp_required'] and level_data['level'] > current_level:
            await db.users.update_one(
                {"id": user_id},
                {"$set": {
                    "level": level_data['level'],
                    "avatar_stage": level_data['avatar_stage']
                }}
            )
            
            # Award level up bonus
            if level_data['points_reward'] > 0:
                await award_points(user_id, level_data['points_reward'], "level_up", f"Reached level {level_data['level']}: {level_data['name']}")
            
            # Create achievement
            achievement = Achievement(
                user_id=user_id,
                title=f"Level {level_data['level']}: {level_data['name']}",
                description=f"Reached level {level_data['level']}!",
                type="level_up",
                icon="⭐"
            )
            doc = achievement.model_dump()
            doc['created_at'] = doc['created_at'].isoformat()
            await db.achievements.insert_one(doc)
            
            break

async def check_and_award_badge(user_id: str, badge_id: str):
    """Check and award badge to user"""
    user = await db.users.find_one({"id": user_id}, {"_id": 0})
    
    # Check if user already has this badge
    if badge_id in user.get('badges', []):
        return
    
    # Find badge details
    badge = next((b for b in BADGE_SYSTEM if b['id'] == badge_id), None)
    if not badge:
        return
    
    # Award badge
    await db.users.update_one(
        {"id": user_id},
        {"$push": {"badges": badge_id}}
    )
    
    # Award points
    await award_points(user_id, badge['points_reward'], "badge_earned", f"Earned badge: {badge['name']}")
    
    # Create achievement
    achievement = Achievement(
        user_id=user_id,
        title=f"Badge Earned: {badge['name']}",
        description=badge['description'],
        type="badge",
        icon=badge['icon']
    )
    doc = achievement.model_dump()
    doc['created_at'] = doc['created_at'].isoformat()
    await db.achievements.insert_one(doc)

# ============= AUTH ROUTES =============

@api_router.post("/auth/register", response_model=TokenResponse)
async def register(user_data: UserCreate):
    existing_user = await db.users.find_one({"email": user_data.email})
    if existing_user:
        raise HTTPException(status_code=400, detail="Email already registered")
    
    user = User(
        email=user_data.email,
        name=user_data.name,
        password_hash=hash_password(user_data.password)
    )
    
    doc = user.model_dump()
    doc['created_at'] = doc['created_at'].isoformat()
    await db.users.insert_one(doc)
    
    access_token = create_access_token({"sub": user.id})
    user_response = UserResponse(
        id=user.id,
        email=user.email,
        name=user.name,
        points=user.points,
        total_workouts=user.total_workouts,
        created_at=user.created_at
    )
    
    return TokenResponse(access_token=access_token, user=user_response)

@api_router.post("/auth/login", response_model=TokenResponse)
async def login(credentials: UserLogin):
    user = await db.users.find_one({"email": credentials.email}, {"_id": 0})
    if not user:
        raise HTTPException(status_code=401, detail="Invalid credentials")
    
    if not verify_password(credentials.password, user['password_hash']):
        raise HTTPException(status_code=401, detail="Invalid credentials")
    
    access_token = create_access_token({"sub": user['id']})
    
    if isinstance(user['created_at'], str):
        user['created_at'] = datetime.fromisoformat(user['created_at'])
    
    user_response = UserResponse(
        id=user['id'],
        email=user['email'],
        name=user['name'],
        points=user.get('points', 0),
        total_workouts=user.get('total_workouts', 0),
        created_at=user['created_at']
    )
    
    return TokenResponse(access_token=access_token, user=user_response)

@api_router.get("/auth/me", response_model=UserResponse)
async def get_me(current_user: dict = Depends(get_current_user)):
    if isinstance(current_user['created_at'], str):
        current_user['created_at'] = datetime.fromisoformat(current_user['created_at'])
    return UserResponse(**current_user)

# ============= MEAL PLANNING ROUTES =============

@api_router.post("/meals/generate")
async def generate_meal_plan(request: MealPlanRequest, current_user: dict = Depends(get_current_user)):
    try:
        groceries_text = ", ".join([f"{item.quantity} {item.unit} {item.name}" for item in request.groceries])
        preferences_text = f"User preferences: {request.preferences}" if request.preferences else ""
        
        prompt = f"""You are a nutrition expert. Create a healthy meal plan based on these available groceries:
{groceries_text}

{preferences_text}

Provide a detailed meal with:
1. Meal name
2. List of ingredients from the available groceries
3. Step-by-step cooking instructions
4. Estimated calories and protein

Format as JSON with keys: meal_name, ingredients (array), instructions, calories, protein"""
        
        chat = LlmChat(
            api_key=os.environ.get('EMERGENT_LLM_KEY'),
            session_id=f"meal_plan_{current_user['id']}_{datetime.now().timestamp()}",
            system_message="You are a helpful nutrition assistant."
        ).with_model("gemini", "gemini-3-flash-preview")
        
        user_message = UserMessage(text=prompt)
        response = await chat.send_message(user_message)
        
        import json
        import re
        try:
            response_text = response.strip()
            if "```json" in response_text:
                response_text = response_text.split("```json")[1].split("```")[0]
            elif "```" in response_text:
                response_text = response_text.split("```")[1].split("```")[0]
            
            meal_data = json.loads(response_text)
            
            def extract_number(value):
                if value is None:
                    return None
                if isinstance(value, (int, float)):
                    return int(value)
                match = re.search(r'\d+', str(value))
                return int(match.group()) if match else None
            
            meal_plan = MealPlan(
                user_id=current_user['id'],
                meal_name=meal_data.get('meal_name', 'Generated Meal'),
                ingredients=meal_data.get('ingredients', []),
                instructions=meal_data.get('instructions', ''),
                calories=extract_number(meal_data.get('calories')),
                protein=extract_number(meal_data.get('protein'))
            )
            
            doc = meal_plan.model_dump()
            doc['created_at'] = doc['created_at'].isoformat()
            await db.meal_plans.insert_one(doc)
            
            return meal_plan
        except json.JSONDecodeError:
            return {"raw_response": response, "message": "Meal plan generated but formatting needs adjustment"}
            
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to generate meal plan: {str(e)}")

@api_router.get("/meals", response_model=List[MealPlan])
async def get_meal_plans(current_user: dict = Depends(get_current_user)):
    meal_plans = await db.meal_plans.find({"user_id": current_user['id']}, {"_id": 0}).sort("created_at", -1).to_list(50)
    
    for plan in meal_plans:
        if isinstance(plan['created_at'], str):
            plan['created_at'] = datetime.fromisoformat(plan['created_at'])
    
    return meal_plans

# ============= GOALS ROUTES =============

@api_router.post("/goals", response_model=Goal)
async def create_goal(goal_data: GoalCreate, current_user: dict = Depends(get_current_user)):
    goal = Goal(
        user_id=current_user['id'],
        title=goal_data.title,
        description=goal_data.description,
        target_value=goal_data.target_value,
        unit=goal_data.unit,
        deadline=goal_data.deadline
    )
    
    doc = goal.model_dump()
    doc['created_at'] = doc['created_at'].isoformat()
    if doc.get('deadline'):
        doc['deadline'] = doc['deadline'].isoformat()
    await db.goals.insert_one(doc)
    
    return goal

@api_router.get("/goals", response_model=List[Goal])
async def get_goals(current_user: dict = Depends(get_current_user)):
    goals = await db.goals.find({"user_id": current_user['id']}, {"_id": 0}).to_list(100)
    
    for goal in goals:
        if isinstance(goal['created_at'], str):
            goal['created_at'] = datetime.fromisoformat(goal['created_at'])
        if goal.get('deadline') and isinstance(goal['deadline'], str):
            goal['deadline'] = datetime.fromisoformat(goal['deadline'])
    
    return goals

@api_router.patch("/goals/{goal_id}", response_model=Goal)
async def update_goal(goal_id: str, update_data: GoalUpdate, current_user: dict = Depends(get_current_user)):
    goal = await db.goals.find_one({"id": goal_id, "user_id": current_user['id']}, {"_id": 0})
    if not goal:
        raise HTTPException(status_code=404, detail="Goal not found")
    
    update_dict = {k: v for k, v in update_data.model_dump().items() if v is not None}
    if update_dict:
        await db.goals.update_one({"id": goal_id}, {"$set": update_dict})
        goal.update(update_dict)
    
    if isinstance(goal['created_at'], str):
        goal['created_at'] = datetime.fromisoformat(goal['created_at'])
    if goal.get('deadline') and isinstance(goal['deadline'], str):
        goal['deadline'] = datetime.fromisoformat(goal['deadline'])
    
    if update_data.status == 'completed':
        await award_points(current_user['id'], 50, "goal_completed", f"Completed goal: {goal['title']}")
        
        achievement = Achievement(
            user_id=current_user['id'],
            title=f"Goal Achieved: {goal['title']}",
            description=f"Completed goal: {goal['description']}",
            type="goal_completion",
            icon="trophy"
        )
        doc = achievement.model_dump()
        doc['created_at'] = doc['created_at'].isoformat()
        await db.achievements.insert_one(doc)
    
    return Goal(**goal)

# ============= GROUP GOALS ROUTES =============

@api_router.post("/groups", response_model=GroupGoal)
async def create_group_goal(group_data: GroupGoalCreate, current_user: dict = Depends(get_current_user)):
    group_goal = GroupGoal(
        name=group_data.name,
        description=group_data.description,
        target_value=group_data.target_value,
        unit=group_data.unit,
        created_by=current_user['id'],
        members=[current_user['id']]
    )
    
    doc = group_goal.model_dump()
    doc['created_at'] = doc['created_at'].isoformat()
    await db.group_goals.insert_one(doc)
    
    return group_goal

@api_router.get("/groups", response_model=List[GroupGoal])
async def get_group_goals(current_user: dict = Depends(get_current_user)):
    group_goals = await db.group_goals.find(
        {"members": current_user['id']},
        {"_id": 0}
    ).to_list(100)
    
    for group in group_goals:
        if isinstance(group['created_at'], str):
            group['created_at'] = datetime.fromisoformat(group['created_at'])
    
    return group_goals

@api_router.post("/groups/{group_id}/join", response_model=GroupGoal)
async def join_group(group_id: str, current_user: dict = Depends(get_current_user)):
    group = await db.group_goals.find_one({"id": group_id}, {"_id": 0})
    if not group:
        raise HTTPException(status_code=404, detail="Group not found")
    
    if current_user['id'] not in group['members']:
        await db.group_goals.update_one(
            {"id": group_id},
            {"$push": {"members": current_user['id']}}
        )
        group['members'].append(current_user['id'])
    
    if isinstance(group['created_at'], str):
        group['created_at'] = datetime.fromisoformat(group['created_at'])
    
    return GroupGoal(**group)

@api_router.patch("/groups/{group_id}/progress")
async def update_group_progress(group_id: str, progress: float, current_user: dict = Depends(get_current_user)):
    group = await db.group_goals.find_one({"id": group_id}, {"_id": 0})
    if not group:
        raise HTTPException(status_code=404, detail="Group not found")
    
    if current_user['id'] not in group['members']:
        raise HTTPException(status_code=403, detail="Not a member of this group")
    
    await db.group_goals.update_one(
        {"id": group_id},
        {"$inc": {"current_value": progress}}
    )
    
    await award_points(current_user['id'], 20, "group_progress", f"Added progress to {group['name']}")
    
    return {"message": "Progress updated", "progress_added": progress}

@api_router.get("/groups/{group_id}/messages", response_model=List[Message])
async def get_messages(group_id: str, current_user: dict = Depends(get_current_user)):
    group = await db.group_goals.find_one({"id": group_id}, {"_id": 0})
    if not group or current_user['id'] not in group['members']:
        raise HTTPException(status_code=403, detail="Access denied")
    
    messages = await db.messages.find(
        {"group_goal_id": group_id},
        {"_id": 0}
    ).sort("created_at", 1).to_list(200)
    
    for msg in messages:
        if isinstance(msg['created_at'], str):
            msg['created_at'] = datetime.fromisoformat(msg['created_at'])
    
    return messages

@api_router.post("/groups/{group_id}/messages", response_model=Message)
async def send_message(group_id: str, message_data: MessageCreate, current_user: dict = Depends(get_current_user)):
    group = await db.group_goals.find_one({"id": group_id}, {"_id": 0})
    if not group or current_user['id'] not in group['members']:
        raise HTTPException(status_code=403, detail="Access denied")
    
    message = Message(
        group_goal_id=group_id,
        user_id=current_user['id'],
        user_name=current_user['name'],
        content=message_data.content
    )
    
    doc = message.model_dump()
    doc['created_at'] = doc['created_at'].isoformat()
    await db.messages.insert_one(doc)
    
    return message

# ============= WEIGHT TRACKING ROUTES =============

@api_router.post("/weight", response_model=WeightEntry)
async def add_weight_entry(entry_data: WeightEntryCreate, current_user: dict = Depends(get_current_user)):
    entry = WeightEntry(
        user_id=current_user['id'],
        weight=entry_data.weight,
        unit=entry_data.unit,
        notes=entry_data.notes
    )
    
    doc = entry.model_dump()
    doc['created_at'] = doc['created_at'].isoformat()
    await db.weight_entries.insert_one(doc)
    
    return entry

@api_router.get("/weight", response_model=List[WeightEntry])
async def get_weight_entries(current_user: dict = Depends(get_current_user)):
    entries = await db.weight_entries.find(
        {"user_id": current_user['id']},
        {"_id": 0}
    ).sort("created_at", -1).to_list(100)
    
    for entry in entries:
        if isinstance(entry['created_at'], str):
            entry['created_at'] = datetime.fromisoformat(entry['created_at'])
    
    return entries

# ============= WORKOUT ROUTES =============

@api_router.get("/workouts/videos")
async def get_workout_videos(category: Optional[str] = None):
    query = {"category": category} if category else {}
    videos = await db.workout_videos.find(query, {"_id": 0}).to_list(100)
    return videos

@api_router.post("/workouts/log", response_model=WorkoutLog)
async def log_workout(workout_data: WorkoutLogCreate, current_user: dict = Depends(get_current_user)):
    points_earned = max(10, workout_data.duration_minutes // 10 * 5)
    
    workout_log = WorkoutLog(
        user_id=current_user['id'],
        workout_video_id=workout_data.workout_video_id,
        workout_type=workout_data.workout_type,
        duration_minutes=workout_data.duration_minutes,
        calories_burned=workout_data.calories_burned,
        notes=workout_data.notes,
        points_earned=points_earned
    )
    
    doc = workout_log.model_dump()
    doc['created_at'] = doc['created_at'].isoformat()
    await db.workout_logs.insert_one(doc)
    
    await db.users.update_one(
        {"id": current_user['id']},
        {"$inc": {"total_workouts": 1}}
    )
    
    await award_points(current_user['id'], points_earned, "workout_completed", f"Completed {workout_data.workout_type} workout")
    
    return workout_log

@api_router.get("/workouts/logs", response_model=List[WorkoutLog])
async def get_workout_logs(current_user: dict = Depends(get_current_user)):
    logs = await db.workout_logs.find(
        {"user_id": current_user['id']},
        {"_id": 0}
    ).sort("created_at", -1).to_list(100)
    
    for log in logs:
        if isinstance(log['created_at'], str):
            log['created_at'] = datetime.fromisoformat(log['created_at'])
    
    return logs

# ============= POINTS & REWARDS ROUTES =============

@api_router.get("/points/balance")
async def get_points_balance(current_user: dict = Depends(get_current_user)):
    return {"points": current_user.get('points', 0)}

@api_router.get("/points/transactions")
async def get_points_transactions(current_user: dict = Depends(get_current_user)):
    transactions = await db.points_transactions.find(
        {"user_id": current_user['id']},
        {"_id": 0}
    ).sort("created_at", -1).to_list(50)
    
    for txn in transactions:
        if isinstance(txn['created_at'], str):
            txn['created_at'] = datetime.fromisoformat(txn['created_at'])
    
    return transactions

@api_router.get("/shop/items")
async def get_shop_items(category: Optional[str] = None):
    query = {"category": category} if category else {}
    items = await db.shop_items.find(query, {"_id": 0}).to_list(100)
    return items

@api_router.post("/shop/purchase", response_model=RewardPurchase)
async def purchase_reward(purchase_data: RewardPurchaseCreate, current_user: dict = Depends(get_current_user)):
    item = await db.shop_items.find_one({"id": purchase_data.shop_item_id}, {"_id": 0})
    if not item:
        raise HTTPException(status_code=404, detail="Item not found")
    
    user_points = current_user.get('points', 0)
    if user_points < item['price_points']:
        raise HTTPException(status_code=400, detail="Insufficient points")
    
    purchase = RewardPurchase(
        user_id=current_user['id'],
        shop_item_id=item['id'],
        item_name=item['name'],
        points_spent=item['price_points']
    )
    
    doc = purchase.model_dump()
    doc['created_at'] = doc['created_at'].isoformat()
    await db.reward_purchases.insert_one(doc)
    
    await db.users.update_one(
        {"id": current_user['id']},
        {"$inc": {"points": -item['price_points']}}
    )
    
    transaction = PointsTransaction(
        user_id=current_user['id'],
        points=-item['price_points'],
        transaction_type="reward_purchase",
        description=f"Purchased {item['name']}"
    )
    txn_doc = transaction.model_dump()
    txn_doc['created_at'] = txn_doc['created_at'].isoformat()
    await db.points_transactions.insert_one(txn_doc)
    
    return purchase

@api_router.get("/shop/purchases")
async def get_purchases(current_user: dict = Depends(get_current_user)):
    purchases = await db.reward_purchases.find(
        {"user_id": current_user['id']},
        {"_id": 0}
    ).sort("created_at", -1).to_list(50)
    
    for purchase in purchases:
        if isinstance(purchase['created_at'], str):
            purchase['created_at'] = datetime.fromisoformat(purchase['created_at'])
    
    return purchases

# ============= ACHIEVEMENTS & SOCIAL SHARING =============

@api_router.get("/achievements")
async def get_achievements(current_user: dict = Depends(get_current_user)):
    achievements = await db.achievements.find(
        {"user_id": current_user['id']},
        {"_id": 0}
    ).sort("created_at", -1).to_list(100)
    
    for achievement in achievements:
        if isinstance(achievement['created_at'], str):
            achievement['created_at'] = datetime.fromisoformat(achievement['created_at'])
    
    return achievements

@api_router.post("/achievements/share")
async def share_achievement(share_data: ShareAchievement, current_user: dict = Depends(get_current_user)):
    achievement = await db.achievements.find_one(
        {"id": share_data.achievement_id, "user_id": current_user['id']},
        {"_id": 0}
    )
    if not achievement:
        raise HTTPException(status_code=404, detail="Achievement not found")
    
    await db.achievements.update_one(
        {"id": share_data.achievement_id},
        {"$set": {"shared": True}}
    )
    
    await award_points(current_user['id'], 5, "social_share", "Shared achievement on social media")
    
    share_url = f"https://innate.fitness/share/{share_data.achievement_id}"
    
    return {
        "message": "Achievement shared",
        "platform": share_data.platform,
        "share_url": share_url,
        "points_earned": 5
    }

# ============= FITNESS INTEGRATION ROUTES =============

@api_router.post("/fitness/sync")
async def sync_fitness_data(current_user: dict = Depends(get_current_user)):
    return {
        "message": "Fitness integration endpoint ready",
        "status": "infrastructure_prepared",
        "supported_sources": ["apple_health", "google_fit", "withings_scale"]
    }

@api_router.get("/fitness/data")
async def get_fitness_data(current_user: dict = Depends(get_current_user)):
    fitness_data = await db.fitness_data.find(
        {"user_id": current_user['id']},
        {"_id": 0}
    ).sort("date", -1).to_list(30)
    
    return fitness_data

@api_router.get("/fitness/scale/connect")
async def connect_scale(current_user: dict = Depends(get_current_user)):
    return {
        "message": "Scale API integration ready",
        "instructions": "Add SCALE_API_KEY and SCALE_API_SECRET to .env",
        "supported_scales": ["Withings Body+", "Fitbit Aria", "QardioBase"]
    }

# ============= DASHBOARD STATS =============

@api_router.get("/dashboard/stats")
async def get_dashboard_stats(current_user: dict = Depends(get_current_user)):
    latest_weight = await db.weight_entries.find_one(
        {"user_id": current_user['id']},
        {"_id": 0},
        sort=[("created_at", -1)]
    )
    
    active_goals = await db.goals.count_documents({
        "user_id": current_user['id'],
        "status": "active"
    })
    
    group_count = await db.group_goals.count_documents({
        "members": current_user['id']
    })
    
    meal_count = await db.meal_plans.count_documents({
        "user_id": current_user['id']
    })
    
    workout_count = await db.workout_logs.count_documents({
        "user_id": current_user['id']
    })
    
    return {
        "latest_weight": latest_weight.get('weight') if latest_weight else None,
        "weight_unit": latest_weight.get('unit') if latest_weight else "kg",
        "active_goals": active_goals,
        "group_memberships": group_count,
        "meal_plans": meal_count,
        "total_workouts": workout_count,
        "points": current_user.get('points', 0)
    }

# ============= SEED DATA ENDPOINT =============

@api_router.post("/seed/shop")
async def seed_shop_data():
    """Seed initial shop items"""
    existing = await db.shop_items.count_documents({})
    if existing > 0:
        return {"message": "Shop already seeded"}
    
    shop_items = [
        {"name": "Innate Performance T-Shirt", "category": "apparel", "price_points": 500, "image_url": "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab", "stock": 100},
        {"name": "Premium Whey Protein (2kg)", "category": "supplements", "price_points": 800, "image_url": "https://images.unsplash.com/photo-1579722820308-d74e571900a9", "stock": 50},
        {"name": "Innate Hoodie", "category": "apparel", "price_points": 1000, "image_url": "https://images.unsplash.com/photo-1556821840-3a63f95609a7", "stock": 75},
        {"name": "Protein Bar Box (12 pack)", "category": "nutrition", "price_points": 300, "image_url": "https://images.unsplash.com/photo-1607623488620-5c79a6545351", "stock": 200},
        {"name": "Creatine Monohydrate", "category": "supplements", "price_points": 400, "image_url": "https://images.unsplash.com/photo-1610497889054-b77df9e67d87", "stock": 100},
        {"name": "Innate Joggers", "category": "apparel", "price_points": 700, "image_url": "https://images.unsplash.com/photo-1555689502-c4b22d76c56f", "stock": 60},
        {"name": "Pre-Workout Energy Drink", "category": "drinks", "price_points": 250, "image_url": "https://images.unsplash.com/photo-1622543925917-763c34f1f161", "stock": 150},
        {"name": "Resistance Bands Set", "category": "equipment", "price_points": 600, "image_url": "https://images.unsplash.com/photo-1598289431512-b97b0917affc", "stock": 80}
    ]
    
    for item_data in shop_items:
        item = ShopItem(id=str(uuid.uuid4()), **item_data)
        await db.shop_items.insert_one(item.model_dump())
    
    return {"message": "Shop seeded successfully", "items_added": len(shop_items)}

@api_router.post("/seed/workouts")
async def seed_workout_videos():
    """Seed initial workout videos"""
    existing = await db.workout_videos.count_documents({})
    if existing > 0:
        return {"message": "Workouts already seeded"}
    
    workout_videos = [
        {"title": "Full Body HIIT", "category": "HIIT", "difficulty": "Intermediate", "duration_minutes": 30, "video_url": "https://youtube.com/watch?v=sample1", "thumbnail_url": "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b", "description": "High-intensity interval training for full body", "points_reward": 30},
        {"title": "Yoga Flow for Beginners", "category": "Yoga", "difficulty": "Beginner", "duration_minutes": 20, "video_url": "https://youtube.com/watch?v=sample2", "thumbnail_url": "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b", "description": "Gentle yoga flow for flexibility", "points_reward": 20},
        {"title": "Strength Training - Upper Body", "category": "Strength", "difficulty": "Advanced", "duration_minutes": 45, "video_url": "https://youtube.com/watch?v=sample3", "thumbnail_url": "https://images.unsplash.com/photo-1534438327276-14e5300c3a48", "description": "Build upper body strength", "points_reward": 45},
        {"title": "Core Workout", "category": "Core", "difficulty": "Intermediate", "duration_minutes": 15, "video_url": "https://youtube.com/watch?v=sample4", "thumbnail_url": "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b", "description": "Strengthen your core muscles", "points_reward": 15},
        {"title": "Cardio Blast", "category": "Cardio", "difficulty": "Intermediate", "duration_minutes": 25, "video_url": "https://youtube.com/watch?v=sample5", "thumbnail_url": "https://images.unsplash.com/photo-1518611012118-696072aa579a", "description": "High-energy cardio session", "points_reward": 25}
    ]
    
    for video_data in workout_videos:
        video = WorkoutVideo(id=str(uuid.uuid4()), **video_data)
        await db.workout_videos.insert_one(video.model_dump())
    
    return {"message": "Workouts seeded successfully", "videos_added": len(workout_videos)}

# Include the router in the main app
app.include_router(api_router)

app.add_middleware(
    CORSMiddleware,
    allow_credentials=True,
    allow_origins=os.environ.get('CORS_ORIGINS', '*').split(','),
    allow_methods=["*"],
    allow_headers=["*"],
)

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

@app.on_event("shutdown")
async def shutdown_db_client():
    client.close()