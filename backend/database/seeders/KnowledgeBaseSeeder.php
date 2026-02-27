<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;

use App\Models\Category;

class KnowledgeBaseSeeder extends Seeder
{
    /**
     * Run the database seeds.
     *
     * @return void
     */
    public function run()
    {
        $categories = [
            'Policies',
            'HR Document',
            'Security',
            'Finance',
            'Management'
        ];

        foreach ($categories as $name) {
            Category::updateOrCreate(['name' => $name]);
        }
    }
}
