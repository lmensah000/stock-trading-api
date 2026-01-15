import { Link, useNavigate } from 'react-router-dom';
import { Home, Target, Users, Utensils, Weight, User, LogOut } from 'lucide-react';
import { Button } from '@/components/ui/button';

export const Navbar = () => {
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  return (
    <nav className="bg-[#18181B] border-b border-[#27272A]" data-testid="navbar">
      <div className="max-w-7xl mx-auto px-4">
        <div className="flex items-center justify-between h-16">
          <Link to="/dashboard" className="flex items-center">
            <h1 className="text-2xl font-black uppercase tracking-tight">
              <span className="text-volt">VOLT</span>FIT
            </h1>
          </Link>

          <div className="flex items-center gap-6">
            <Link to="/dashboard">
              <Button variant="ghost" size="sm" className="text-[#A1A1AA] hover:text-white" data-testid="nav-dashboard">
                <Home className="w-4 h-4 mr-2" />
                Dashboard
              </Button>
            </Link>
            <Link to="/meals">
              <Button variant="ghost" size="sm" className="text-[#A1A1AA] hover:text-white" data-testid="nav-meals">
                <Utensils className="w-4 h-4 mr-2" />
                Meals
              </Button>
            </Link>
            <Link to="/goals">
              <Button variant="ghost" size="sm" className="text-[#A1A1AA] hover:text-white" data-testid="nav-goals">
                <Target className="w-4 h-4 mr-2" />
                Goals
              </Button>
            </Link>
            <Link to="/groups">
              <Button variant="ghost" size="sm" className="text-[#A1A1AA] hover:text-white" data-testid="nav-groups">
                <Users className="w-4 h-4 mr-2" />
                Groups
              </Button>
            </Link>
            <Link to="/weight">
              <Button variant="ghost" size="sm" className="text-[#A1A1AA] hover:text-white" data-testid="nav-weight">
                <Weight className="w-4 h-4 mr-2" />
                Weight
              </Button>
            </Link>
            <Link to="/profile">
              <Button variant="ghost" size="icon" className="text-[#A1A1AA] hover:text-white" data-testid="nav-profile">
                <User className="w-4 h-4" />
              </Button>
            </Link>
            <Button 
              variant="ghost" 
              size="icon" 
              onClick={handleLogout}
              className="text-[#A1A1AA] hover:text-[#FF3300]"
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