import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Plus, TrendingUp, Share2, Trophy } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Progress } from '@/components/ui/progress';
import { toast } from 'sonner';
import apiClient from '@/api';
import Navbar from '@/components/Navbar';
import ShareModal from '@/components/ShareModal';

export default function Goals() {
  const [goals, setGoals] = useState([]);
  const [achievements, setAchievements] = useState([]);
  const [open, setOpen] = useState(false);
  const [shareModalOpen, setShareModalOpen] = useState(false);
  const [selectedAchievement, setSelectedAchievement] = useState(null);
  const [newGoal, setNewGoal] = useState({
    title: '',
    description: '',
    target_value: '',
    unit: '',
    deadline: ''
  });

  useEffect(() => {
    fetchGoals();
    fetchAchievements();
  }, []);

  const fetchGoals = async () => {
    try {
      const response = await apiClient.get('/goals');
      setGoals(response.data);
    } catch (error) {
      console.error('Failed to fetch goals:', error);
    }
  };

  const fetchAchievements = async () => {
    try {
      const response = await apiClient.get('/achievements');
      setAchievements(response.data);
    } catch (error) {
      console.error('Failed to fetch achievements:', error);
    }
  };

  const createGoal = async (e) => {
    e.preventDefault();
    try {
      await apiClient.post('/goals', {
        ...newGoal,
        target_value: parseFloat(newGoal.target_value),
        deadline: newGoal.deadline || null
      });
      
      toast.success('Goal created!');
      setOpen(false);
      setNewGoal({ title: '', description: '', target_value: '', unit: '', deadline: '' });
      fetchGoals();
    } catch (error) {
      toast.error('Failed to create goal');
    }
  };

  const updateProgress = async (goalId, newValue) => {
    try {
      await apiClient.patch(`/goals/${goalId}`, {
        current_value: parseFloat(newValue)
      });
      toast.success('Progress updated!');
      fetchGoals();
    } catch (error) {
      toast.error('Failed to update progress');
    }
  };

  const markComplete = async (goalId) => {
    try {
      await apiClient.patch(`/goals/${goalId}`, {
        status: 'completed'
      });
      toast.success('Goal completed! 🎉');
      fetchGoals();
    } catch (error) {
      toast.error('Failed to update goal');
    }
  };

  return (
    <div className="min-h-screen bg-[#09090B]">
      <Navbar />
      
      <div className="max-w-7xl mx-auto px-4 py-8" data-testid="goals-page">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-4xl md:text-6xl font-black uppercase tracking-tight">
            YOUR <span className="text-volt">GOALS</span>
          </h1>
          
          <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
              <Button 
                data-testid="create-goal-btn"
                className="bg-[#CCFF00] text-black font-bold uppercase hover:bg-[#B3E600]"
              >
                <Plus className="w-4 h-4 mr-2" />
                NEW GOAL
              </Button>
            </DialogTrigger>
            <DialogContent className="bg-[#18181B] border border-[#27272A] text-white">
              <DialogHeader>
                <DialogTitle className="text-2xl font-bold uppercase">CREATE GOAL</DialogTitle>
              </DialogHeader>
              <form onSubmit={createGoal} className="space-y-4" data-testid="goal-form">
                <div>
                  <Label className="text-white uppercase text-xs font-bold tracking-widest mb-2 block">
                    Goal Title
                  </Label>
                  <Input
                    data-testid="goal-title-input"
                    value={newGoal.title}
                    onChange={(e) => setNewGoal({ ...newGoal, title: e.target.value })}
                    className="bg-[#09090B] border-[#27272A] text-white focus:border-[#CCFF00]"
                    required
                  />
                </div>
                
                <div>
                  <Label className="text-white uppercase text-xs font-bold tracking-widest mb-2 block">
                    Description
                  </Label>
                  <Textarea
                    data-testid="goal-description-input"
                    value={newGoal.description}
                    onChange={(e) => setNewGoal({ ...newGoal, description: e.target.value })}
                    className="bg-[#09090B] border-[#27272A] text-white focus:border-[#CCFF00]"
                    required
                  />
                </div>
                
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <Label className="text-white uppercase text-xs font-bold tracking-widest mb-2 block">
                      Target Value
                    </Label>
                    <Input
                      data-testid="goal-target-input"
                      type="number"
                      value={newGoal.target_value}
                      onChange={(e) => setNewGoal({ ...newGoal, target_value: e.target.value })}
                      className="bg-[#09090B] border-[#27272A] text-white focus:border-[#CCFF00]"
                      required
                    />
                  </div>
                  <div>
                    <Label className="text-white uppercase text-xs font-bold tracking-widest mb-2 block">
                      Unit
                    </Label>
                    <Input
                      data-testid="goal-unit-input"
                      value={newGoal.unit}
                      onChange={(e) => setNewGoal({ ...newGoal, unit: e.target.value })}
                      placeholder="kg, reps, km"
                      className="bg-[#09090B] border-[#27272A] text-white focus:border-[#CCFF00]"
                      required
                    />
                  </div>
                </div>
                
                <div>
                  <Label className="text-white uppercase text-xs font-bold tracking-widest mb-2 block">
                    Deadline (Optional)
                  </Label>
                  <Input
                    data-testid="goal-deadline-input"
                    type="date"
                    value={newGoal.deadline}
                    onChange={(e) => setNewGoal({ ...newGoal, deadline: e.target.value })}
                    className="bg-[#09090B] border-[#27272A] text-white focus:border-[#CCFF00]"
                  />
                </div>
                
                <Button
                  type="submit"
                  data-testid="submit-goal-btn"
                  className="w-full bg-[#CCFF00] text-black font-bold uppercase hover:bg-[#B3E600]"
                >
                  CREATE GOAL
                </Button>
              </form>
            </DialogContent>
          </Dialog>
        </div>

        {/* Goals List */}
        <div className="grid md:grid-cols-2 gap-6" data-testid="goals-list">
          {goals.length === 0 ? (
            <div className="md:col-span-2 bg-[#18181B] border border-[#27272A] p-8 text-center">
              <p className="text-[#A1A1AA]">No goals yet. Create your first goal to get started!</p>
            </div>
          ) : (
            goals.map((goal) => {
              const progress = (goal.current_value / goal.target_value) * 100;
              const isCompleted = goal.status === 'completed';
              
              return (
                <motion.div
                  key={goal.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  className={`bg-[#18181B] border p-6 ${
                    isCompleted ? 'border-[#CCFF00]' : 'border-[#27272A] hover:border-[#CCFF00]/50'
                  } transition-all duration-300`}
                  data-testid={`goal-${goal.id}`}
                >
                  {isCompleted && (
                    <div className="mb-4">
                      <span className="inline-block bg-[#CCFF00] text-black text-xs font-bold uppercase px-3 py-1">
                        COMPLETED
                      </span>
                    </div>
                  )}
                  
                  <h3 className="text-2xl font-bold uppercase mb-2">{goal.title}</h3>
                  <p className="text-[#A1A1AA] mb-4">{goal.description}</p>
                  
                  <div className="mb-4">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-white font-bold">
                        {goal.current_value} / {goal.target_value} {goal.unit}
                      </span>
                      <span className="text-[#CCFF00] font-bold">{progress.toFixed(0)}%</span>
                    </div>
                    <Progress value={progress} className="h-2 bg-[#27272A]" />
                  </div>
                  
                  {goal.deadline && (
                    <p className="text-[#A1A1AA] text-sm mb-4">
                      Deadline: {new Date(goal.deadline).toLocaleDateString()}
                    </p>
                  )}
                  
                  {!isCompleted && (
                    <div className="flex gap-2">
                      <Input
                        type="number"
                        placeholder="Update progress"
                        data-testid={`update-progress-${goal.id}`}
                        className="bg-[#09090B] border-[#27272A] text-white focus:border-[#CCFF00]"
                        onKeyPress={(e) => {
                          if (e.key === 'Enter') {
                            updateProgress(goal.id, e.target.value);
                            e.target.value = '';
                          }
                        }}
                      />
                      {progress >= 100 && (
                        <Button
                          onClick={() => markComplete(goal.id)}
                          data-testid={`complete-goal-${goal.id}`}
                          className="bg-[#CCFF00] text-black font-bold uppercase hover:bg-[#B3E600] whitespace-nowrap"
                        >
                          <TrendingUp className="w-4 h-4 mr-2" />
                          COMPLETE
                        </Button>
                      )}
                    </div>
                  )}
                </motion.div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
}