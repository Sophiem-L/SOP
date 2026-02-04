<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use App\Models\Notification;
use App\Models\User;

class NotificationSeeder extends Seeder
{
    public function run()
    {
        $admin = User::where('email', 'admin@example.com')->first();
        $employee = User::where('email', 'employee1@example.com')->first();

        if ($admin) {
            $n1 = Notification::create([
                'title' => 'Welcome to the System',
                'message' => 'Your admin account has been successfully created.',
                'type' => 'success',
            ]);
            $admin->notifications()->attach($n1->id, ['is_read' => false]);

            $n2 = Notification::create([
                'title' => 'New Feedback Received',
                'message' => 'A user has submitted new feedback on a document.',
                'type' => 'info',
            ]);
            $admin->notifications()->attach($n2->id, ['is_read' => false]);
        }

        if ($employee) {
            $n3 = Notification::create([
                'title' => 'Assigned to New SOP',
                'message' => 'You have been assigned to read the "Safety Protocols" SOP.',
                'type' => 'warning',
            ]);
            $employee->notifications()->attach($n3->id, ['is_read' => false]);
        }
    }
}
