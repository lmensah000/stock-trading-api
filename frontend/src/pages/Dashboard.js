import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import { Activity, Target, Users, Utensils, Weight, TrendingUp, Dumbbell, Award, Trophy } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { LineChart, Line, AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import apiClient from '@/api';
import Navbar from '@/components/Navbar';
import WelcomeModal from '@/components/WelcomeModal';

export default function Dashboard({ user }) {
  const [stats, setStats] = useState(null);
  const [weightData, setWeightData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showWelcome, setShowWelcome] = useState(false);

  useEffect(() => {
    fetchDashboardData();
    
    const hasSeenWelcome = localStorage.getItem('hasSeenWelcome');
    if (!hasSeenWelcome) {
      setShowWelcome(true);
      localStorage.setItem('hasSeenWelcome', 'true');
    }
  }, []);

  const fetchDashboardData = async () => {
    try {
      const [statsRes, weightRes] = await Promise.all([
        apiClient.get('/dashboard/stats'),
        apiClient.get('/weight')
      ]);
      
      setStats(statsRes.data);
      
      const formattedWeightData = weightRes.data.slice(0, 10).reverse().map(entry => ({
        date: new Date(entry.created_at).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
        weight: entry.weight
      }));
      setWeightData(formattedWeightData);
    } catch (error) {
      console.error('Failed to fetch dashboard data:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-[#F5F3EF] flex items-center justify-center">
        <div className="text-[#D4A574] text-2xl font-bold uppercase">LOADING...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#F5F3EF]">
      <Navbar />
      
      <div className="max-w-7xl mx-auto px-4 py-8" data-testid="dashboard">
        {/* Header */}
        <div className="mb-12">
          <h1 className="text-4xl md:text-6xl font-black uppercase tracking-tight mb-2 text-[#5C4A42]">
            WELCOME BACK, <span className="text-[#D4A574]">{user?.name?.split(' ')[0]?.toUpperCase() || 'ATHLETE'}</span>
          </h1>
          <p className="text-[#8B7355] text-lg italic">Your transformation starts from within</p>
        </div>

        {/* Bento Grid Layout */}
        <div className="bento-grid">
          {/* Points Card */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="warm-card p-6 rounded-lg bg-gradient-to-br from-[#D4A574] to-[#8B7355] text-white"
            data-testid="points-card"
          >
            <div className="flex items-center justify-between mb-4">
              <Award className="w-8 h-8" strokeWidth={1.5} />
              <Link to="/shop">
                <Button variant="ghost" size="sm" className="text-white hover:bg-white/20">
                  SHOP
                </Button>
              </Link>
            </div>
            <div className="text-5xl font-black mb-1">{stats?.points || 0}</div>
            <p className="uppercase text-sm tracking-widest opacity-90">Reward Points</p>
          </motion.div>

          {/* Weight Card */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
            className="warm-card p-6 rounded-lg"
            data-testid="weight-card"
          >
            <div className="flex items-center justify-between mb-4">
              <Weight className="w-8 h-8 text-[#D4A574]" strokeWidth={1.5} />
              <span className="text-xs uppercase font-bold tracking-widest text-[#A89284]">CURRENT</span>
            </div>
            <div className="text-4xl font-black mb-1 text-[#5C4A42]">
              {stats?.latest_weight || '--'}
              <span className="text-2xl text-[#8B7355] ml-2">{stats?.weight_unit || 'kg'}</span>
            </div>
            <p className="text-[#8B7355] text-sm uppercase">Body Weight</p>
          </motion.div>

          {/* Workouts Card */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
            className="warm-card p-6 rounded-lg"
            data-testid="workouts-card"
          >
            <div className="flex items-center justify-between mb-4">
              <Dumbbell className="w-8 h-8 text-[#8B7355]" strokeWidth={1.5} />
              <Link to="/workouts">
                <Button variant="ghost" size="sm" className="text-[#D4A574] hover:bg-[#FAF9F7]">
                  VIEW
                </Button>
              </Link>
            </div>
            <div className="text-4xl font-black mb-1 text-[#5C4A42]">{stats?.total_workouts || 0}</div>
            <p className="text-[#8B7355] text-sm uppercase">Total Workouts</p>
          </motion.div>

          {/* Active Goals Card */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 }}
            className="warm-card p-6 rounded-lg"
            data-testid="goals-card"
          >
            <div className="flex items-center justify-between mb-4">
              <Target className="w-8 h-8 text-[#D4A574]" strokeWidth={1.5} />
              <Link to="/goals">
                <Button variant="ghost" size="sm" className="text-[#D4A574] hover:bg-[#FAF9F7]">
                  VIEW
                </Button>
              </Link>
            </div>
            <div className="text-4xl font-black mb-1 text-[#5C4A42]">{stats?.active_goals || 0}</div>
            <p className="text-[#8B7355] text-sm uppercase">Active Goals</p>
          </motion.div>

          {/* Group Goals Card */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.4 }}
            className="warm-card p-6 rounded-lg"
            data-testid="groups-card"
          >
            <div className="flex items-center justify-between mb-4">
              <Users className="w-8 h-8 text-[#8B7355]" strokeWidth={1.5} />
              <Link to="/groups">
                <Button variant="ghost" size="sm" className="text-[#D4A574] hover:bg-[#FAF9F7]">
                  VIEW
                </Button>
              </Link>
            </div>
            <div className="text-4xl font-black mb-1 text-[#5C4A42]">{stats?.group_memberships || 0}</div>
            <p className="text-[#8B7355] text-sm uppercase">Group Challenges</p>
          </motion.div>

          {/* Meal Plans Card */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.5 }}
            className="warm-card p-6 rounded-lg"
            data-testid="meals-card"
          >
            <div className="flex items-center justify-between mb-4">
              <Utensils className="w-8 h-8 text-[#D4A574]" strokeWidth={1.5} />
              <Link to="/meals">
                <Button variant="ghost" size="sm" className="text-[#D4A574] hover:bg-[#FAF9F7]">
                  VIEW
                </Button>
              </Link>
            </div>
            <div className="text-4xl font-black mb-1 text-[#5C4A42]">{stats?.meal_plans || 0}</div>
            <p className="text-[#8B7355] text-sm uppercase">Meal Plans</p>
          </motion.div>

          {/* Weight Progress Chart */}
          {weightData.length > 0 && (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.6 }}
              className="bento-2x1 warm-card p-6 rounded-lg"
              data-testid="weight-chart"
            >
              <div className="flex items-center justify-between mb-6">
                <div>
                  <h3 className="text-xl font-bold uppercase text-[#5C4A42]">WEIGHT PROGRESS</h3>
                  <p className="text-[#8B7355] text-sm">Last 10 entries</p>
                </div>
                <TrendingUp className="w-6 h-6 text-[#D4A574]" strokeWidth={1.5} />
              </div>
              <div className="chart-container" style={{ width: '100%', height: 200 }}>
                <ResponsiveContainer>
                  <AreaChart data={weightData}>
                    <defs>
                      <linearGradient id="weightGradient" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#D4A574" stopOpacity={0.3}/>
                        <stop offset="95%" stopColor="#D4A574" stopOpacity={0}/>
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" stroke="#E8E3DC" />
                    <XAxis dataKey="date" stroke="#8B7355" style={{ fontSize: 12 }} />
                    <YAxis stroke="#8B7355" style={{ fontSize: 12 }} />
                    <Tooltip 
                      contentStyle={{ backgroundColor: '#FFFFFF', border: '1px solid #D4C4B0', borderRadius: '8px' }}
                      labelStyle={{ color: '#5C4A42' }}
                      itemStyle={{ color: '#D4A574' }}
                    />
                    <Area type="monotone" dataKey="weight" stroke="#D4A574" strokeWidth={2} fill="url(#weightGradient)" />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </motion.div>
          )}

          {/* Quick Actions */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.7 }}
            className="bento-2x1 warm-card p-6 rounded-lg"
            data-testid="quick-actions"
          >
            <h3 className="text-xl font-bold uppercase mb-6 text-[#5C4A42]">QUICK ACTIONS</h3>
            <div className="grid grid-cols-2 gap-4">
              <Link to="/workouts">
                <Button 
                  data-testid="start-workout-btn"
                  className="w-full bg-[#D4A574] text-white font-bold uppercase hover:bg-[#C19563] transition-all duration-300"
                >
                  <Dumbbell className="w-4 h-4 mr-2" />
                  START WORKOUT
                </Button>
              </Link>
              <Link to="/meals">
                <Button 
                  data-testid="generate-meal-btn"
                  variant="outline"
                  className="w-full border-[#D4C4B0] text-[#8B7355] hover:bg-[#FAF9F7] uppercase"
                >
                  <Utensils className="w-4 h-4 mr-2" />
                  NEW MEAL
                </Button>
              </Link>
              <Link to="/weight">
                <Button 
                  data-testid="log-weight-btn"
                  variant="outline"
                  className="w-full border-[#D4C4B0] text-[#8B7355] hover:bg-[#FAF9F7] uppercase"
                >
                  <Weight className="w-4 h-4 mr-2" />
                  LOG WEIGHT
                </Button>
              </Link>
              <Link to="/goals">
                <Button 
                  data-testid="add-goal-btn"
                  variant="outline"
                  className="w-full border-[#D4C4B0] text-[#8B7355] hover:bg-[#FAF9F7] uppercase"
                >
                  <Target className="w-4 h-4 mr-2" />
                  ADD GOAL
                </Button>
              </Link>
            </div>
          </motion.div>
        </div>
      </div>

      <WelcomeModal open={showWelcome} onClose={() => setShowWelcome(false)} />
    </div>
  );
}