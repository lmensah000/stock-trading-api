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
    members: List[str]  # user IDs
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
    source: str = "manual"  # manual, scale_api
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
    source: str  # apple_health, google_fit, manual
    steps: Optional[int] = None
    calories_burned: Optional[int] = None
    distance: Optional[float] = None
    active_minutes: Optional[int] = None
    date: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))

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
        
        # Parse the response and save to database
        import json
        try:
            # Extract JSON from response
            response_text = response.strip()
            if "```json" in response_text:
                response_text = response_text.split("```json")[1].split("```")[0]
            elif "```" in response_text:
                response_text = response_text.split("```")[1].split("```")[0]
            
            meal_data = json.loads(response_text)
            
            # Parse calories and protein, extracting numeric values from strings like "580 kcal" or "25g"
            def extract_number(value):
                if value is None:
                    return None
                if isinstance(value, (int, float)):
                    return int(value)
                # Extract first number from string
                import re
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
            # If parsing fails, return raw response
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
    
    return {"message": "Progress updated", "progress_added": progress}

@api_router.get("/groups/{group_id}/messages", response_model=List[Message])
async def get_messages(group_id: str, current_user: dict = Depends(get_current_user)):
    # Verify user is member
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
    # Verify user is member
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

# ============= FITNESS INTEGRATION ROUTES =============

@api_router.post("/fitness/sync")
async def sync_fitness_data(current_user: dict = Depends(get_current_user)):
    """Infrastructure ready for Apple/Google Fitness integration"""
    return {
        "message": "Fitness integration endpoint ready",
        "status": "infrastructure_prepared",
        "supported_sources": ["apple_health", "google_fit", "withings_scale"]
    }

@api_router.get("/fitness/data")
async def get_fitness_data(current_user: dict = Depends(get_current_user)):
    """Get fitness data - ready for integration"""
    fitness_data = await db.fitness_data.find(
        {"user_id": current_user['id']},
        {"_id": 0}
    ).sort("date", -1).to_list(30)
    
    return fitness_data

@api_router.get("/fitness/scale/connect")
async def connect_scale(current_user: dict = Depends(get_current_user)):
    """Infrastructure for digital scale API (Withings/Fitbit Aria)"""
    return {
        "message": "Scale API integration ready",
        "instructions": "Add SCALE_API_KEY and SCALE_API_SECRET to .env",
        "supported_scales": ["Withings Body+", "Fitbit Aria", "QardioBase"]
    }

# ============= DASHBOARD STATS =============

@api_router.get("/dashboard/stats")
async def get_dashboard_stats(current_user: dict = Depends(get_current_user)):
    # Get latest weight
    latest_weight = await db.weight_entries.find_one(
        {"user_id": current_user['id']},
        {"_id": 0},
        sort=[("created_at", -1)]
    )
    
    # Count active goals
    active_goals = await db.goals.count_documents({
        "user_id": current_user['id'],
        "status": "active"
    })
    
    # Count group memberships
    group_count = await db.group_goals.count_documents({
        "members": current_user['id']
    })
    
    # Count meal plans
    meal_count = await db.meal_plans.count_documents({
        "user_id": current_user['id']
    })
    
    return {
        "latest_weight": latest_weight.get('weight') if latest_weight else None,
        "weight_unit": latest_weight.get('unit') if latest_weight else "kg",
        "active_goals": active_goals,
        "group_memberships": group_count,
        "meal_plans": meal_count
    }

# Include the router in the main app
app.include_router(api_router)

app.add_middleware(
    CORSMiddleware,
    allow_credentials=True,
    allow_origins=os.environ.get('CORS_ORIGINS', '*').split(','),
    allow_methods=["*"],
    allow_headers=["*"],
)

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

@app.on_event("shutdown")
async def shutdown_db_client():
    client.close()