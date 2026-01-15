import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Award, Package, ShoppingBag, Star } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { toast } from 'sonner';
import apiClient from '@/api';
import Navbar from '@/components/Navbar';

export default function Shop() {
  const [items, setItems] = useState([]);
  const [purchases, setPurchases] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [userPoints, setUserPoints] = useState(0);
  const [selectedItem, setSelectedItem] = useState(null);

  useEffect(() => {
    fetchShopItems();
    fetchPurchases();
    fetchUserPoints();
    seedShop();
  }, []);

  const seedShop = async () => {
    try {
      await apiClient.post('/seed/shop');
    } catch (error) {
      console.log('Shop already seeded');
    }
  };

  const fetchShopItems = async () => {
    try {
      const response = await apiClient.get('/shop/items');
      setItems(response.data);
    } catch (error) {
      console.error('Failed to fetch shop items:', error);
    }
  };

  const fetchPurchases = async () => {
    try {
      const response = await apiClient.get('/shop/purchases');
      setPurchases(response.data);
    } catch (error) {
      console.error('Failed to fetch purchases:', error);
    }
  };

  const fetchUserPoints = async () => {
    try {
      const response = await apiClient.get('/points/balance');
      setUserPoints(response.data.points);
    } catch (error) {
      console.error('Failed to fetch points:', error);
    }
  };

  const handlePurchase = async (itemId) => {
    try {
      await apiClient.post('/shop/purchase', { shop_item_id: itemId });
      toast.success('Purchase successful! Check your email for details.');
      fetchPurchases();
      fetchUserPoints();
      
      const userStr = localStorage.getItem('user');
      if (userStr) {
        const response = await apiClient.get('/auth/me');
        localStorage.setItem('user', JSON.stringify(response.data));
        window.location.reload();
      }
    } catch (error) {
      toast.error(error.response?.data?.detail || 'Purchase failed');
    }
  };

  const filteredItems = selectedCategory === 'all' 
    ? items 
    : items.filter(item => item.category === selectedCategory);

  const categories = ['all', ...new Set(items.map(item => item.category))];

  return (
    <div className="min-h-screen bg-[#F5F3EF]">
      <Navbar />
      
      <div className="max-w-7xl mx-auto px-4 py-8" data-testid="shop-page">
        <div className="mb-8">
          <h1 className="text-4xl md:text-6xl font-black uppercase tracking-tight text-[#5C4A42]">
            REWARDS SHOP
          </h1>
          <p className="text-[#8B7355] mt-2">Redeem your points for premium gear and supplements</p>
          <div className="mt-4 inline-flex items-center gap-3 px-6 py-3 bg-gradient-to-r from-[#D4A574] to-[#8B7355] text-white rounded-lg innate-shadow">
            <Award className="w-6 h-6" />
            <div>
              <p className="text-xs uppercase tracking-wide opacity-90">Your Balance</p>
              <p className="text-2xl font-black">{userPoints} Points</p>
            </div>
          </div>
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
          {/* Shop Items */}
          <div>
            <h2 className="text-2xl font-bold uppercase mb-6 text-[#5C4A42]">AVAILABLE REWARDS</h2>
            <div className="grid gap-6" data-testid="shop-items">
              {filteredItems.length === 0 ? (
                <div className="warm-card p-8 rounded-lg text-center">
                  <p className="text-[#8B7355]">No items available in this category</p>
                </div>
              ) : (
                filteredItems.map((item) => (
                  <motion.div
                    key={item.id}
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="warm-card p-6 rounded-lg"
                    data-testid={`item-${item.id}`}
                  >
                    <div className="flex gap-4">
                      <div className="w-32 h-32 flex-shrink-0 rounded overflow-hidden bg-[#FAF9F7]">
                        <img 
                          src={item.image_url} 
                          alt={item.name}
                          className="w-full h-full object-cover"
                        />
                      </div>
                      <div className="flex-1">
                        <h3 className="text-xl font-bold text-[#5C4A42] mb-2">{item.name}</h3>
                        <p className="text-sm text-[#8B7355] mb-3">{item.description}</p>
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-2 text-[#D4A574] font-bold text-lg">
                            <Award className="w-5 h-5" />
                            {item.price_points} pts
                          </div>
                          <Button
                            onClick={() => setSelectedItem(item)}
                            disabled={userPoints < item.price_points}
                            className={userPoints >= item.price_points
                              ? "bg-[#D4A574] text-white hover:bg-[#C19563]"
                              : "bg-[#D4C4B0] text-[#A89284] cursor-not-allowed"}
                            data-testid={`redeem-${item.id}`}
                          >
                            <ShoppingBag className="w-4 h-4 mr-2" />
                            REDEEM
                          </Button>
                        </div>
                      </div>
                    </div>
                  </motion.div>
                ))
              )}
            </div>
          </div>

          {/* Purchase History */}
          <div>
            <h2 className="text-2xl font-bold uppercase mb-6 text-[#5C4A42]">YOUR REWARDS</h2>
            <div className="space-y-4" data-testid="purchase-history">
              {purchases.length === 0 ? (
                <div className="warm-card p-8 rounded-lg text-center">
                  <Package className="w-12 h-12 text-[#D4C4B0] mx-auto mb-3" />
                  <p className="text-[#8B7355]">No purchases yet. Start earning points!</p>
                </div>
              ) : (
                purchases.map((purchase) => (
                  <div
                    key={purchase.id}
                    className="warm-card p-4 rounded-lg"
                    data-testid={`purchase-${purchase.id}`}
                  >
                    <div className="flex items-center justify-between mb-2">
                      <h3 className="font-bold text-[#5C4A42]">{purchase.item_name}</h3>
                      <span className={`text-xs uppercase font-bold px-2 py-1 rounded ${
                        purchase.status === 'pending' ? 'bg-[#FFF4E6] text-[#D4A574]' : 'bg-[#E8F5E9] text-[#4CAF50]'
                      }`}>
                        {purchase.status}
                      </span>
                    </div>
                    <div className="flex justify-between text-sm text-[#8B7355]">
                      <span>{purchase.points_spent} points</span>
                      <span className="text-[#A89284]">
                        {new Date(purchase.created_at).toLocaleDateString()}
                      </span>
                    </div>
                  </div>
                ))
              )}
            </div>

            {/* Points Info */}
            <div className="mt-8 warm-card p-6 rounded-lg bg-gradient-to-br from-[#FAF9F7] to-white">
              <h3 className="font-bold text-[#5C4A42] mb-4 uppercase">Earn More Points</h3>
              <ul className="space-y-2 text-sm text-[#8B7355]">
                <li className="flex items-center gap-2">
                  <Star className="w-4 h-4 text-[#D4A574]" />
                  Complete workouts: 10-50 points
                </li>
                <li className="flex items-center gap-2">
                  <Star className="w-4 h-4 text-[#D4A574]" />
                  Complete goals: 50 points
                </li>
                <li className="flex items-center gap-2">
                  <Star className="w-4 h-4 text-[#D4A574]" />
                  Group challenge progress: 20 points
                </li>
                <li className="flex items-center gap-2">
                  <Star className="w-4 h-4 text-[#D4A574]" />
                  Share achievements: 5 points
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>

      {/* Purchase Confirmation Dialog */}
      {selectedItem && (
        <Dialog open={!!selectedItem} onOpenChange={() => setSelectedItem(null)}>
          <DialogContent className="bg-white border border-[#D4C4B0] text-[#5C4A42]">
            <DialogHeader>
              <DialogTitle className="text-2xl font-bold uppercase">CONFIRM PURCHASE</DialogTitle>
            </DialogHeader>
            <div className="space-y-4">
              <div className="flex gap-4">
                <img 
                  src={selectedItem.image_url} 
                  alt={selectedItem.name}
                  className="w-24 h-24 object-cover rounded"
                />
                <div>
                  <h3 className="font-bold text-lg text-[#5C4A42]">{selectedItem.name}</h3>
                  <p className="text-sm text-[#8B7355]">{selectedItem.description}</p>
                </div>
              </div>
              <div className="flex justify-between items-center py-4 border-t border-[#D4C4B0]">
                <span className="text-[#8B7355]">Cost:</span>
                <span className="text-2xl font-bold text-[#D4A574] flex items-center gap-2">
                  <Award className="w-6 h-6" />
                  {selectedItem.price_points} points
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-[#8B7355]">Your Balance:</span>
                <span className="font-bold text-[#5C4A42]">{userPoints} points</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-[#8B7355]">After Purchase:</span>
                <span className="font-bold text-[#5C4A42]">{userPoints - selectedItem.price_points} points</span>
              </div>
              <Button
                onClick={() => {
                  handlePurchase(selectedItem.id);
                  setSelectedItem(null);
                }}
                className="w-full bg-[#D4A574] text-white font-bold uppercase hover:bg-[#C19563]"
              >
                CONFIRM PURCHASE
              </Button>
            </div>
          </DialogContent>
        </Dialog>
      )}
    </div>
  );
}
