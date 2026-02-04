<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;

use App\Models\Article;
use App\Models\Sop;
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

        Article::create([
            'title' => 'Getting Started with Knowledge Base',
            'content' => 'This is a sample article providing an overview of how to use our system efficiently.',
            'category' => 'General'
        ]);

        Sop::create([
            'title' => 'Creating a New SOP',
            'steps' => '1. Research the process.\n2. Document each step.\n3. Review with stakeholders.\n4. Publish.',
            'department' => 'Operations'
        ]);
    }
}
