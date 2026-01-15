import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import { Activity, Target, Users, Utensils, Weight, TrendingUp } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { LineChart, Line, AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import apiClient from '@/api';
import Navbar from '@/components/Navbar';

export default function Dashboard({ user }) {
  const [stats, setStats] = useState(null);
  const [weightData, setWeightData] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    try {
      const [statsRes, weightRes] = await Promise.all([
        apiClient.get('/dashboard/stats'),
        apiClient.get('/weight')
      ]);
      
      setStats(statsRes.data);
      
      // Format weight data for chart
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
      <div className="min-h-screen bg-[#09090B] flex items-center justify-center">
        <div className="text-[#CCFF00] text-2xl font-bold uppercase">LOADING...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#09090B]">
      <Navbar />
      
      <div className="max-w-7xl mx-auto px-4 py-8" data-testid="dashboard">
        {/* Header */}
        <div className="mb-12">
          <h1 className="text-4xl md:text-6xl font-black uppercase tracking-tight mb-2">
            WELCOME BACK, <span className="text-volt">{user?.name?.split(' ')[0] || 'ATHLETE'}</span>
          </h1>
          <p className="text-[#A1A1AA] text-lg">Your performance dashboard</p>
        </div>

        {/* Bento Grid Layout */}
        <div className="bento-grid">
          {/* Weight Card */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="bg-[#18181B] border border-[#27272A] p-6 hover:border-[#CCFF00]/50 transition-all duration-300"
            data-testid="weight-card"
          >
            <div className="flex items-center justify-between mb-4">
              <Weight className="w-8 h-8 text-[#CCFF00]" strokeWidth={1.5} />
              <span className="text-xs uppercase font-bold tracking-widest text-[#52525B]">CURRENT</span>
            </div>
            <div className="text-4xl font-black mb-1">
              {stats?.latest_weight || '--'}
              <span className="text-2xl text-[#A1A1AA] ml-2">{stats?.weight_unit || 'kg'}</span>
            </div>
            <p className="text-[#A1A1AA] text-sm uppercase">Body Weight</p>
          </motion.div>

          {/* Active Goals Card */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
            className="bg-[#18181B] border border-[#27272A] p-6 hover:border-[#CCFF00]/50 transition-all duration-300"
            data-testid="goals-card"
          >
            <div className="flex items-center justify-between mb-4">
              <Target className="w-8 h-8 text-[#00E0FF]" strokeWidth={1.5} />
              <Link to="/goals">
                <Button variant="ghost" size="sm" className="text-[#CCFF00] hover:text-[#B3E600]">
                  VIEW
                </Button>
              </Link>
            </div>
            <div className="text-4xl font-black mb-1">{stats?.active_goals || 0}</div>
            <p className="text-[#A1A1AA] text-sm uppercase">Active Goals</p>
          </motion.div>

          {/* Group Goals Card */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
            className="bg-[#18181B] border border-[#27272A] p-6 hover:border-[#CCFF00]/50 transition-all duration-300"
            data-testid="groups-card"
          >
            <div className="flex items-center justify-between mb-4">
              <Users className="w-8 h-8 text-[#CCFF00]" strokeWidth={1.5} />
              <Link to="/groups">
                <Button variant="ghost" size="sm" className="text-[#CCFF00] hover:text-[#B3E600]">
                  VIEW
                </Button>
              </Link>
            </div>
            <div className="text-4xl font-black mb-1">{stats?.group_memberships || 0}</div>
            <p className="text-[#A1A1AA] text-sm uppercase">Group Challenges</p>
          </motion.div>

          {/* Meal Plans Card */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 }}
            className="bg-[#18181B] border border-[#27272A] p-6 hover:border-[#CCFF00]/50 transition-all duration-300"
            data-testid="meals-card"
          >
            <div className="flex items-center justify-between mb-4">
              <Utensils className="w-8 h-8 text-[#00E0FF]" strokeWidth={1.5} />
              <Link to="/meals">
                <Button variant="ghost" size="sm" className="text-[#CCFF00] hover:text-[#B3E600]">
                  VIEW
                </Button>
              </Link>
            </div>
            <div className="text-4xl font-black mb-1">{stats?.meal_plans || 0}</div>
            <p className="text-[#A1A1AA] text-sm uppercase">Meal Plans</p>
          </motion.div>

          {/* Weight Progress Chart */}
          {weightData.length > 0 && (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.4 }}
              className="bento-2x1 bg-[#18181B] border border-[#27272A] p-6"
              data-testid="weight-chart"
            >
              <div className="flex items-center justify-between mb-6">
                <div>
                  <h3 className="text-xl font-bold uppercase">WEIGHT PROGRESS</h3>
                  <p className="text-[#A1A1AA] text-sm">Last 10 entries</p>
                </div>
                <TrendingUp className="w-6 h-6 text-[#CCFF00]" strokeWidth={1.5} />
              </div>
              <div className="chart-container" style={{ width: '100%', height: 200 }}>
                <ResponsiveContainer>
                  <AreaChart data={weightData}>
                    <defs>
                      <linearGradient id="weightGradient" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#CCFF00" stopOpacity={0.3}/>
                        <stop offset="95%" stopColor="#CCFF00" stopOpacity={0}/>
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" stroke="#27272A" />
                    <XAxis dataKey="date" stroke="#A1A1AA" style={{ fontSize: 12 }} />
                    <YAxis stroke="#A1A1AA" style={{ fontSize: 12 }} />
                    <Tooltip 
                      contentStyle={{ backgroundColor: '#18181B', border: '1px solid #27272A', borderRadius: '4px' }}
                      labelStyle={{ color: '#FFFFFF' }}
                      itemStyle={{ color: '#CCFF00' }}
                    />
                    <Area type="monotone" dataKey="weight" stroke="#CCFF00" strokeWidth={2} fill="url(#weightGradient)" />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </motion.div>
          )}

          {/* Quick Actions */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.5 }}
            className="bento-2x1 bg-[#18181B] border border-[#27272A] p-6"
            data-testid="quick-actions"
          >
            <h3 className="text-xl font-bold uppercase mb-6">QUICK ACTIONS</h3>
            <div className="grid grid-cols-2 gap-4">
              <Link to="/meals">
                <Button 
                  data-testid="generate-meal-btn"
                  className="w-full bg-[#CCFF00] text-black font-bold uppercase hover:bg-[#B3E600] transition-all duration-300"
                >
                  <Utensils className="w-4 h-4 mr-2" />
                  NEW MEAL
                </Button>
              </Link>
              <Link to="/weight">
                <Button 
                  data-testid="log-weight-btn"
                  variant="outline"
                  className="w-full border-[#27272A] text-white hover:border-[#CCFF00] hover:text-[#CCFF00] uppercase"
                >
                  <Weight className="w-4 h-4 mr-2" />
                  LOG WEIGHT
                </Button>
              </Link>
              <Link to="/goals">
                <Button 
                  data-testid="add-goal-btn"
                  variant="outline"
                  className="w-full border-[#27272A] text-white hover:border-[#CCFF00] hover:text-[#CCFF00] uppercase"
                >
                  <Target className="w-4 h-4 mr-2" />
                  ADD GOAL
                </Button>
              </Link>
              <Link to="/groups">
                <Button 
                  data-testid="join-group-btn"
                  variant="outline"
                  className="w-full border-[#27272A] text-white hover:border-[#CCFF00] hover:text-[#CCFF00] uppercase"
                >
                  <Users className="w-4 h-4 mr-2" />
                  JOIN GROUP
                </Button>
              </Link>
            </div>
          </motion.div>
        </div>
      </div>
    </div>
  );
}