import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Plus, Trash2, Sparkles } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { toast } from 'sonner';
import apiClient from '@/api';
import Navbar from '@/components/Navbar';

export default function MealPlanning() {
  const [groceries, setGroceries] = useState([]);
  const [newGrocery, setNewGrocery] = useState({ name: '', quantity: '', unit: 'pcs' });
  const [preferences, setPreferences] = useState('');
  const [mealPlans, setMealPlans] = useState([]);
  const [generating, setGenerating] = useState(false);

  useEffect(() => {
    fetchMealPlans();
  }, []);

  const fetchMealPlans = async () => {
    try {
      const response = await apiClient.get('/meals');
      setMealPlans(response.data);
    } catch (error) {
      console.error('Failed to fetch meal plans:', error);
    }
  };

  const addGrocery = () => {
    if (newGrocery.name && newGrocery.quantity) {
      setGroceries([...groceries, { ...newGrocery, id: Date.now().toString() }]);
      setNewGrocery({ name: '', quantity: '', unit: 'pcs' });
    }
  };

  const removeGrocery = (id) => {
    setGroceries(groceries.filter(g => g.id !== id));
  };

  const generateMealPlan = async () => {
    if (groceries.length === 0) {
      toast.error('Please add at least one grocery item');
      return;
    }

    setGenerating(true);
    try {
      const response = await apiClient.post('/meals/generate', {
        groceries,
        preferences
      });
      
      toast.success('Meal plan generated!');
      fetchMealPlans();
      setGroceries([]);
      setPreferences('');
    } catch (error) {
      toast.error(error.response?.data?.detail || 'Failed to generate meal plan');
    } finally {
      setGenerating(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#09090B]">
      <Navbar />
      
      <div className="max-w-7xl mx-auto px-4 py-8" data-testid="meal-planning-page">
        <h1 className="text-4xl md:text-6xl font-black uppercase tracking-tight mb-8">
          AI <span className="text-volt">MEAL PLANNING</span>
        </h1>

        <div className="grid md:grid-cols-2 gap-8">
          {/* Grocery Input Section */}
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            className="bg-[#18181B] border border-[#27272A] p-6"
          >
            <h2 className="text-2xl font-bold uppercase mb-6">AVAILABLE GROCERIES</h2>
            
            <div className="space-y-4 mb-6">
              <div>
                <Label className="text-white uppercase text-xs font-bold tracking-widest mb-2 block">
                  Item Name
                </Label>
                <Input
                  data-testid="grocery-name-input"
                  value={newGrocery.name}
                  onChange={(e) => setNewGrocery({ ...newGrocery, name: e.target.value })}
                  placeholder="e.g., Chicken Breast"
                  className="bg-[#09090B] border-[#27272A] text-white focus:border-[#CCFF00]"
                  onKeyPress={(e) => e.key === 'Enter' && addGrocery()}
                />
              </div>
              
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label className="text-white uppercase text-xs font-bold tracking-widest mb-2 block">
                    Quantity
                  </Label>
                  <Input
                    data-testid="grocery-quantity-input"
                    value={newGrocery.quantity}
                    onChange={(e) => setNewGrocery({ ...newGrocery, quantity: e.target.value })}
                    placeholder="2"
                    className="bg-[#09090B] border-[#27272A] text-white focus:border-[#CCFF00]"
                  />
                </div>
                <div>
                  <Label className="text-white uppercase text-xs font-bold tracking-widest mb-2 block">
                    Unit
                  </Label>
                  <Input
                    data-testid="grocery-unit-input"
                    value={newGrocery.unit}
                    onChange={(e) => setNewGrocery({ ...newGrocery, unit: e.target.value })}
                    placeholder="kg"
                    className="bg-[#09090B] border-[#27272A] text-white focus:border-[#CCFF00]"
                  />
                </div>
              </div>
              
              <Button
                onClick={addGrocery}
                data-testid="add-grocery-btn"
                className="w-full bg-[#CCFF00] text-black font-bold uppercase hover:bg-[#B3E600]"
              >
                <Plus className="w-4 h-4 mr-2" />
                ADD ITEM
              </Button>
            </div>

            {/* Grocery List */}
            <div className="space-y-2 mb-6" data-testid="grocery-list">
              {groceries.map((grocery) => (
                <div
                  key={grocery.id}
                  className="flex items-center justify-between bg-[#27272A] p-3 border border-[#3F3F46]"
                >
                  <span className="text-white">
                    {grocery.quantity} {grocery.unit} {grocery.name}
                  </span>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => removeGrocery(grocery.id)}
                    className="text-[#FF3300] hover:text-[#FF3300]/80"
                    data-testid={`remove-grocery-${grocery.id}`}
                  >
                    <Trash2 className="w-4 h-4" />
                  </Button>
                </div>
              ))}
            </div>

            {/* Preferences */}
            <div className="mb-6">
              <Label className="text-white uppercase text-xs font-bold tracking-widest mb-2 block">
                PREFERENCES (OPTIONAL)
              </Label>
              <Textarea
                data-testid="preferences-input"
                value={preferences}
                onChange={(e) => setPreferences(e.target.value)}
                placeholder="e.g., High protein, low carb, vegetarian..."
                className="bg-[#09090B] border-[#27272A] text-white focus:border-[#CCFF00] min-h-[100px]"
              />
            </div>

            <Button
              onClick={generateMealPlan}
              disabled={generating || groceries.length === 0}
              data-testid="generate-meal-plan-btn"
              className="w-full bg-[#00E0FF] text-black font-bold uppercase hover:bg-[#00C8E6] transition-all duration-300"
            >
              <Sparkles className="w-4 h-4 mr-2" />
              {generating ? 'GENERATING...' : 'GENERATE MEAL PLAN'}
            </Button>
          </motion.div>

          {/* Meal Plans History */}
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            className="space-y-4"
            data-testid="meal-plans-list"
          >
            <h2 className="text-2xl font-bold uppercase mb-6">YOUR MEAL PLANS</h2>
            
            {mealPlans.length === 0 ? (
              <div className="bg-[#18181B] border border-[#27272A] p-8 text-center">
                <p className="text-[#A1A1AA]">No meal plans yet. Generate your first one!</p>
              </div>
            ) : (
              mealPlans.map((plan) => (
                <div
                  key={plan.id}
                  className="bg-[#18181B] border border-[#27272A] p-6 hover:border-[#CCFF00]/50 transition-all duration-300"
                  data-testid={`meal-plan-${plan.id}`}
                >
                  <h3 className="text-xl font-bold text-[#CCFF00] mb-3 uppercase">{plan.meal_name}</h3>
                  
                  {plan.calories && plan.protein && (
                    <div className="flex gap-4 mb-4">
                      <span className="text-[#A1A1AA] text-sm uppercase">
                        <span className="text-white font-bold">{plan.calories}</span> CAL
                      </span>
                      <span className="text-[#A1A1AA] text-sm uppercase">
                        <span className="text-white font-bold">{plan.protein}g</span> PROTEIN
                      </span>
                    </div>
                  )}
                  
                  <div className="mb-4">
                    <p className="text-[#A1A1AA] text-xs uppercase font-bold mb-2">INGREDIENTS:</p>
                    <ul className="list-disc list-inside text-white space-y-1">
                      {plan.ingredients.map((ingredient, idx) => (
                        <li key={idx} className="text-sm">{ingredient}</li>
                      ))}
                    </ul>
                  </div>
                  
                  <div>
                    <p className="text-[#A1A1AA] text-xs uppercase font-bold mb-2">INSTRUCTIONS:</p>
                    <p className="text-white text-sm whitespace-pre-line">{plan.instructions}</p>
                  </div>
                </div>
              ))
            )}
          </motion.div>
        </div>
      </div>
    </div>
  );
}