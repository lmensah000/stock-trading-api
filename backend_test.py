import requests
import sys
import json
from datetime import datetime

class VoltFitAPITester:
    def __init__(self, base_url="https://smart-fitness-102.preview.emergentagent.com/api"):
        self.base_url = base_url
        self.token = None
        self.user_id = None
        self.tests_run = 0
        self.tests_passed = 0
        self.test_results = []

    def log_test(self, name, success, details=""):
        """Log test result"""
        self.tests_run += 1
        if success:
            self.tests_passed += 1
        
        result = {
            "test": name,
            "status": "PASS" if success else "FAIL",
            "details": details,
            "timestamp": datetime.now().isoformat()
        }
        self.test_results.append(result)
        
        status_icon = "✅" if success else "❌"
        print(f"{status_icon} {name}: {details}")

    def run_test(self, name, method, endpoint, expected_status, data=None, headers=None):
        """Run a single API test"""
        url = f"{self.base_url}/{endpoint}"
        test_headers = {'Content-Type': 'application/json'}
        
        if self.token:
            test_headers['Authorization'] = f'Bearer {self.token}'
        
        if headers:
            test_headers.update(headers)

        try:
            if method == 'GET':
                response = requests.get(url, headers=test_headers, timeout=30)
            elif method == 'POST':
                response = requests.post(url, json=data, headers=test_headers, timeout=30)
            elif method == 'PATCH':
                response = requests.patch(url, json=data, headers=test_headers, timeout=30)

            success = response.status_code == expected_status
            
            if success:
                try:
                    response_data = response.json()
                    details = f"Status: {response.status_code}"
                    if isinstance(response_data, dict) and len(str(response_data)) < 200:
                        details += f", Response: {response_data}"
                except:
                    details = f"Status: {response.status_code}, Response: {response.text[:100]}"
            else:
                details = f"Expected {expected_status}, got {response.status_code}. Response: {response.text[:200]}"

            self.log_test(name, success, details)
            return success, response.json() if success and response.text else {}

        except Exception as e:
            self.log_test(name, False, f"Error: {str(e)}")
            return False, {}

    def test_user_registration(self):
        """Test user registration"""
        test_user_data = {
            "email": f"test_user_{datetime.now().strftime('%H%M%S')}@voltfit.com",
            "name": "Test User",
            "password": "TestPass123!"
        }
        
        success, response = self.run_test(
            "User Registration",
            "POST",
            "auth/register",
            200,
            data=test_user_data
        )
        
        if success and 'access_token' in response:
            self.token = response['access_token']
            self.user_id = response['user']['id']
            return True, test_user_data
        return False, test_user_data

    def test_user_login(self, user_data):
        """Test user login"""
        login_data = {
            "email": user_data["email"],
            "password": user_data["password"]
        }
        
        success, response = self.run_test(
            "User Login",
            "POST",
            "auth/login",
            200,
            data=login_data
        )
        
        if success and 'access_token' in response:
            self.token = response['access_token']
            return True
        return False

    def test_get_user_profile(self):
        """Test get current user profile"""
        return self.run_test(
            "Get User Profile",
            "GET",
            "auth/me",
            200
        )[0]

    def test_dashboard_stats(self):
        """Test dashboard statistics"""
        return self.run_test(
            "Dashboard Stats",
            "GET",
            "dashboard/stats",
            200
        )[0]

    def test_meal_planning(self):
        """Test AI meal planning functionality"""
        # Test meal generation
        meal_request = {
            "groceries": [
                {"name": "Chicken Breast", "quantity": "2", "unit": "pieces"},
                {"name": "Rice", "quantity": "1", "unit": "cup"},
                {"name": "Broccoli", "quantity": "200", "unit": "g"}
            ],
            "preferences": "High protein, low carb"
        }
        
        success, response = self.run_test(
            "Generate Meal Plan",
            "POST",
            "meals/generate",
            200,
            data=meal_request
        )
        
        # Test get meal plans
        self.run_test(
            "Get Meal Plans",
            "GET",
            "meals",
            200
        )
        
        return success

    def test_personal_goals(self):
        """Test personal goals CRUD operations"""
        # Create goal
        goal_data = {
            "title": "Lose Weight",
            "description": "Lose 5kg in 3 months",
            "target_value": 5.0,
            "unit": "kg",
            "deadline": "2025-04-01"
        }
        
        success, response = self.run_test(
            "Create Personal Goal",
            "POST",
            "goals",
            200,
            data=goal_data
        )
        
        goal_id = None
        if success and 'id' in response:
            goal_id = response['id']
        
        # Get goals
        self.run_test(
            "Get Personal Goals",
            "GET",
            "goals",
            200
        )
        
        # Update goal progress
        if goal_id:
            update_data = {"current_value": 1.5}
            self.run_test(
                "Update Goal Progress",
                "PATCH",
                f"goals/{goal_id}",
                200,
                data=update_data
            )
        
        return success

    def test_group_goals(self):
        """Test group goals functionality"""
        # Create group goal
        group_data = {
            "name": "Team Fitness Challenge",
            "description": "Collective weight loss challenge",
            "target_value": 50.0,
            "unit": "kg"
        }
        
        success, response = self.run_test(
            "Create Group Goal",
            "POST",
            "groups",
            200,
            data=group_data
        )
        
        group_id = None
        if success and 'id' in response:
            group_id = response['id']
        
        # Get group goals
        self.run_test(
            "Get Group Goals",
            "GET",
            "groups",
            200
        )
        
        # Test messaging if group was created
        if group_id:
            # Send message
            message_data = {"content": "Hello team! Let's reach our goal!"}
            self.run_test(
                "Send Group Message",
                "POST",
                f"groups/{group_id}/messages",
                200,
                data=message_data
            )
            
            # Get messages
            self.run_test(
                "Get Group Messages",
                "GET",
                f"groups/{group_id}/messages",
                200
            )
        
        return success

    def test_weight_tracking(self):
        """Test weight tracking functionality"""
        # Add weight entry
        weight_data = {
            "weight": 75.5,
            "unit": "kg",
            "notes": "Feeling good today!"
        }
        
        success, response = self.run_test(
            "Add Weight Entry",
            "POST",
            "weight",
            200,
            data=weight_data
        )
        
        # Get weight entries
        self.run_test(
            "Get Weight Entries",
            "GET",
            "weight",
            200
        )
        
        return success

    def test_fitness_integration(self):
        """Test fitness integration endpoints"""
        # Test sync endpoint
        self.run_test(
            "Fitness Sync Endpoint",
            "POST",
            "fitness/sync",
            200
        )
        
        # Test get fitness data
        self.run_test(
            "Get Fitness Data",
            "GET",
            "fitness/data",
            200
        )
        
        # Test scale connection
        return self.run_test(
            "Scale Connection Endpoint",
            "GET",
            "fitness/scale/connect",
            200
        )[0]

    def run_all_tests(self):
        """Run comprehensive test suite"""
        print("🚀 Starting VoltFit API Test Suite...")
        print("=" * 50)
        
        # Test authentication flow
        reg_success, user_data = self.test_user_registration()
        if not reg_success:
            print("❌ Registration failed, stopping tests")
            return False
        
        login_success = self.test_user_login(user_data)
        if not login_success:
            print("❌ Login failed, stopping tests")
            return False
        
        # Test authenticated endpoints
        self.test_get_user_profile()
        self.test_dashboard_stats()
        
        # Test core features
        print("\n📊 Testing Core Features...")
        self.test_meal_planning()
        self.test_personal_goals()
        self.test_group_goals()
        self.test_weight_tracking()
        self.test_fitness_integration()
        
        # Print summary
        print("\n" + "=" * 50)
        print(f"📈 Test Results: {self.tests_passed}/{self.tests_run} passed")
        success_rate = (self.tests_passed / self.tests_run) * 100 if self.tests_run > 0 else 0
        print(f"📊 Success Rate: {success_rate:.1f}%")
        
        if success_rate < 80:
            print("⚠️  Warning: Low success rate detected")
        
        return success_rate >= 80

def main():
    tester = VoltFitAPITester()
    success = tester.run_all_tests()
    
    # Save detailed results
    with open('/app/test_reports/backend_test_results.json', 'w') as f:
        json.dump({
            "summary": {
                "total_tests": tester.tests_run,
                "passed_tests": tester.tests_passed,
                "success_rate": (tester.tests_passed / tester.tests_run) * 100 if tester.tests_run > 0 else 0,
                "timestamp": datetime.now().isoformat()
            },
            "detailed_results": tester.test_results
        }, f, indent=2)
    
    return 0 if success else 1

if __name__ == "__main__":
    sys.exit(main())