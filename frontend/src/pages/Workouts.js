import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Play, Clock, TrendingUp, Dumbbell, Plus } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { toast } from 'sonner';
import apiClient from '@/api';
import Navbar from '@/components/Navbar';

export default function Workouts() {
  const [videos, setVideos] = useState([]);
  const [logs, setLogs] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [open, setOpen] = useState(false);
  const [logData, setLogData] = useState({
    workout_type: '',
    duration_minutes: '',
    calories_burned: '',
    notes: ''
  });

  useEffect(() => {
    fetchVideos();
    fetchLogs();
    seedWorkouts();
  }, []);

  const seedWorkouts = async () => {
    try {
      await apiClient.post('/seed/workouts');
    } catch (error) {
      console.log('Workouts already seeded');
    }
  };

  const fetchVideos = async () => {
    try {
      const response = await apiClient.get('/workouts/videos');
      setVideos(response.data);
    } catch (error) {
      console.error('Failed to fetch videos:', error);
    }
  };

  const fetchLogs = async () => {
    try {
      const response = await apiClient.get('/workouts/logs');
      setLogs(response.data);
    } catch (error) {
      console.error('Failed to fetch logs:', error);
    }
  };

  const logWorkout = async (e) => {
    e.preventDefault();
    try {
      await apiClient.post('/workouts/log', {
        ...logData,
        duration_minutes: parseInt(logData.duration_minutes),
        calories_burned: logData.calories_burned ? parseInt(logData.calories_burned) : null
      });
      
      toast.success('Workout logged! Points earned!');
      setOpen(false);
      setLogData({ workout_type: '', duration_minutes: '', calories_burned: '', notes: '' });
      fetchLogs();
      
      const userStr = localStorage.getItem('user');
      if (userStr) {
        const user = JSON.parse(userStr);
        const response = await apiClient.get('/auth/me');
        localStorage.setItem('user', JSON.stringify(response.data));
      }
    } catch (error) {
      toast.error('Failed to log workout');
    }
  };

  const filteredVideos = selectedCategory === 'all' 
    ? videos 
    : videos.filter(v => v.category === selectedCategory);

  const categories = ['all', ...new Set(videos.map(v => v.category))];

  return (
    <div className="min-h-screen bg-[#F5F3EF]">
      <Navbar />
      
      <div className="max-w-7xl mx-auto px-4 py-8" data-testid="workouts-page">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-4xl md:text-6xl font-black uppercase tracking-tight text-[#5C4A42]">
              WORKOUT LIBRARY
            </h1>
            <p className="text-[#8B7355] mt-2">Watch guided videos and track your progress</p>
          </div>
          
          <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
              <Button 
                data-testid="log-workout-btn"
                className="bg-[#D4A574] text-white font-bold uppercase hover:bg-[#C19563]"
              >
                <Plus className="w-4 h-4 mr-2" />
                LOG WORKOUT
              </Button>
            </DialogTrigger>
            <DialogContent className="bg-white border border-[#D4C4B0] text-[#5C4A42]">
              <DialogHeader>
                <DialogTitle className="text-2xl font-bold uppercase">LOG WORKOUT</DialogTitle>
              </DialogHeader>
              <form onSubmit={logWorkout} className="space-y-4" data-testid="workout-form">
                <div>
                  <Label className="text-[#5C4A42] uppercase text-xs font-bold tracking-widest mb-2 block">
                    Workout Type
                  </Label>
                  <Input
                    data-testid="workout-type-input"
                    value={logData.workout_type}
                    onChange={(e) => setLogData({ ...logData, workout_type: e.target.value })}
                    placeholder="e.g., Running, Yoga, Strength Training"
                    className="bg-[#FAF9F7] border-[#D4C4B0] text-[#5C4A42] focus:border-[#D4A574]"
                    required
                  />
                </div>
                
                <div>
                  <Label className="text-[#5C4A42] uppercase text-xs font-bold tracking-widest mb-2 block">
                    Duration (minutes)
                  </Label>
                  <Input
                    data-testid="duration-input"
                    type="number"
                    value={logData.duration_minutes}
                    onChange={(e) => setLogData({ ...logData, duration_minutes: e.target.value })}
                    className="bg-[#FAF9F7] border-[#D4C4B0] text-[#5C4A42] focus:border-[#D4A574]"
                    required
                  />
                </div>
                
                <div>
                  <Label className="text-[#5C4A42] uppercase text-xs font-bold tracking-widest mb-2 block">
                    Calories Burned (Optional)
                  </Label>
                  <Input
                    data-testid="calories-input"
                    type="number"
                    value={logData.calories_burned}
                    onChange={(e) => setLogData({ ...logData, calories_burned: e.target.value })}
                    className="bg-[#FAF9F7] border-[#D4C4B0] text-[#5C4A42] focus:border-[#D4A574]"
                  />
                </div>
                
                <div>
                  <Label className="text-[#5C4A42] uppercase text-xs font-bold tracking-widest mb-2 block">
                    Notes (Optional)
                  </Label>
                  <Textarea
                    data-testid="notes-input"
                    value={logData.notes}
                    onChange={(e) => setLogData({ ...logData, notes: e.target.value })}
                    className="bg-[#FAF9F7] border-[#D4C4B0] text-[#5C4A42] focus:border-[#D4A574]"
                    placeholder="How did it feel?"
                  />
                </div>
                
                <Button
                  type="submit"
                  data-testid="submit-workout-btn"
                  className="w-full bg-[#D4A574] text-white font-bold uppercase hover:bg-[#C19563]"
                >
                  LOG WORKOUT
                </Button>
              </form>
            </DialogContent>
          </Dialog>
        </div>

        {/* Category Filter */}
        <div className="mb-8 flex gap-2 flex-wrap">
          {categories.map((cat) => (
            <Button
              key={cat}
              onClick={() => setSelectedCategory(cat)}
              variant={selectedCategory === cat ? "default" : "outline"}
              className={selectedCategory === cat 
                ? "bg-[#D4A574] text-white hover:bg-[#C19563]" 
                : "border-[#D4C4B0] text-[#8B7355] hover:bg-[#FAF9F7]"}
            >
              {cat.toUpperCase()}
            </Button>
          ))}
        </div>

        <div className="grid md:grid-cols-2 gap-8">
          {/* Video Library */}
          <div>
            <h2 className="text-2xl font-bold uppercase mb-6 text-[#5C4A42]">VIDEO LIBRARY</h2>
            <div className="space-y-4" data-testid="video-list">
              {filteredVideos.length === 0 ? (
                <div className="warm-card p-8 rounded-lg text-center">
                  <p className="text-[#8B7355]">No workout videos available</p>
                </div>
              ) : (
                filteredVideos.map((video) => (
                  <motion.div
                    key={video.id}
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="warm-card p-6 rounded-lg"
                    data-testid={`video-${video.id}`}
                  >
                    <div className="flex gap-4">
                      <div className="relative w-32 h-24 flex-shrink-0 rounded overflow-hidden">
                        <img 
                          src={video.thumbnail_url} 
                          alt={video.title}
                          className="w-full h-full object-cover"
                        />
                        <div className="absolute inset-0 bg-black/40 flex items-center justify-center">
                          <Play className="w-8 h-8 text-white" />
                        </div>
                      </div>
                      <div className="flex-1">
                        <h3 className="text-lg font-bold text-[#5C4A42] mb-1">{video.title}</h3>
                        <p className="text-sm text-[#8B7355] mb-2">{video.description}</p>
                        <div className="flex gap-4 text-xs text-[#A89284]">
                          <span className="flex items-center gap-1">
                            <Clock className="w-3 h-3" />
                            {video.duration_minutes} min
                          </span>
                          <span className="uppercase">{video.difficulty}</span>
                          <span className="text-[#D4A574] font-bold">+{video.points_reward} pts</span>
                        </div>
                      </div>
                    </div>
                  </motion.div>
                ))
              )}
            </div>
          </div>

          {/* Workout History */}
          <div>
            <h2 className="text-2xl font-bold uppercase mb-6 text-[#5C4A42]">YOUR WORKOUTS</h2>
            <div className="space-y-4" data-testid="workout-logs">
              {logs.length === 0 ? (
                <div className="warm-card p-8 rounded-lg text-center">
                  <Dumbbell className="w-12 h-12 text-[#D4C4B0] mx-auto mb-3" />
                  <p className="text-[#8B7355]">No workouts logged yet. Start your journey!</p>
                </div>
              ) : (
                logs.map((log) => (
                  <div
                    key={log.id}
                    className="warm-card p-4 rounded-lg"
                    data-testid={`log-${log.id}`}
                  >
                    <div className="flex items-center justify-between mb-2">
                      <h3 className="font-bold text-[#5C4A42]">{log.workout_type}</h3>
                      <span className="text-xs text-[#D4A574] font-bold">+{log.points_earned} pts</span>
                    </div>
                    <div className="flex gap-4 text-sm text-[#8B7355]">
                      <span>{log.duration_minutes} min</span>
                      {log.calories_burned && <span>{log.calories_burned} cal</span>}
                      <span className="text-[#A89284]">
                        {new Date(log.created_at).toLocaleDateString()}
                      </span>
                    </div>
                    {log.notes && (
                      <p className="text-sm text-[#8B7355] mt-2 italic">{log.notes}</p>
                    )}
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}