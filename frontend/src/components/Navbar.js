import { Link, useNavigate } from 'react-router-dom';
import { Home, Target, Users, Utensils, Weight, User, LogOut, Dumbbell, Award, TrendingUp } from 'lucide-react';
import { Button } from '@/components/ui/button';

const LOGO_URL = 'https://customer-assets.emergentagent.com/job_smart-fitness-102/artifacts/xmes01dq_Photoroom_20251009_212303.PNG';
const AVATAR_EMOJIS = ['🌱', '🌿', '🪴', '🌸', '🌺', '🌻', '✨', '🌟'];

export const Navbar = () => {
  const navigate = useNavigate();
  const user = JSON.parse(localStorage.getItem('user') || '{}');

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  return (
    <nav className="bg-white border-b border-[#D4C4B0] innate-shadow" data-testid="navbar">
      <div className="max-w-7xl mx-auto px-4">
        <div className="flex items-center justify-between h-16">
          <Link to="/dashboard" className="flex items-center gap-3">
            <img src={LOGO_URL} alt="Innate" className="h-10" />
            <div>
              <h1 className="text-xl font-black uppercase tracking-tight text-[#5C4A42]">
                INNATE
              </h1>
              <p className="text-xs text-[#8B7355] -mt-1">From within</p>
            </div>
          </Link>

          <div className="flex items-center gap-4">
            <Link to="/dashboard">
              <Button variant="ghost" size="sm" className="text-[#8B7355] hover:text-[#5C4A42] hover:bg-[#FAF9F7]" data-testid="nav-dashboard">
                <Home className="w-4 h-4 mr-2" />
                Dashboard
              </Button>
            </Link>
            <Link to="/workouts">
              <Button variant="ghost" size="sm" className="text-[#8B7355] hover:text-[#5C4A42] hover:bg-[#FAF9F7]" data-testid="nav-workouts">
                <Dumbbell className="w-4 h-4 mr-2" />
                Workouts
              </Button>
            </Link>
            <Link to="/meals">
              <Button variant="ghost" size="sm" className="text-[#8B7355] hover:text-[#5C4A42] hover:bg-[#FAF9F7]" data-testid="nav-meals">
                <Utensils className="w-4 h-4 mr-2" />
                Meals
              </Button>
            </Link>
            <Link to="/goals">
              <Button variant="ghost" size="sm" className="text-[#8B7355] hover:text-[#5C4A42] hover:bg-[#FAF9F7]" data-testid="nav-goals">
                <Target className="w-4 h-4 mr-2" />
                Goals
              </Button>
            </Link>
            <Link to="/groups">
              <Button variant="ghost" size="sm" className="text-[#8B7355] hover:text-[#5C4A42] hover:bg-[#FAF9F7]" data-testid="nav-groups">
                <Users className="w-4 h-4 mr-2" />
                Groups
              </Button>
            </Link>
            <Link to="/shop">
              <Button variant="ghost" size="sm" className="text-[#8B7355] hover:text-[#5C4A42] hover:bg-[#FAF9F7]" data-testid="nav-shop">
                <Award className="w-4 h-4 mr-2" />
                Shop
              </Button>
            </Link>
            
            <div className="h-6 w-px bg-[#D4C4B0]"></div>
            
            {/* Level & Avatar */}
            <Link to="/progress">
              <Button variant="ghost" size="sm" className="text-[#8B7355] hover:text-[#5C4A42] hover:bg-[#FAF9F7]" data-testid="nav-progress">
                <span className="text-xl mr-1">{AVATAR_EMOJIS[(user.avatar_stage || 1) - 1]}</span>
                <span className="text-xs font-bold">LV {user.level || 1}</span>
              </Button>
            </Link>
            
            {/* Points Badge */}
            <div className="flex items-center gap-2 px-3 py-1 bg-gradient-to-r from-[#D4A574] to-[#8B7355] text-white rounded-full">
              <Award className="w-4 h-4" />
              <span className="font-bold text-sm">{user.points || 0}</span>
            </div>
            
            <Link to="/profile">
              <Button variant="ghost" size="icon" className="text-[#8B7355] hover:text-[#5C4A42] hover:bg-[#FAF9F7]" data-testid="nav-profile">
                <User className="w-4 h-4" />
              </Button>
            </Link>
            <Button 
              variant="ghost" 
              size="icon" 
              onClick={handleLogout}
              className="text-[#8B7355] hover:text-[#FF6B6B] hover:bg-[#FAF9F7]"
              data-testid="logout-btn"
            >
              <LogOut className="w-4 h-4" />
            </Button>
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;