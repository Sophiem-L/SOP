<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\DB;
use Carbon\Carbon;

class PermissionSeeder extends Seeder
{
    /**
     * Run the database seeds.
     */
    public function run(): void
    {
        $actions = ['VIEW', 'CREATE', 'EDIT', 'DELETE', 'BOOKMARK'];
        $now = Carbon::now();
        $data = [];
        foreach ($actions as $action) {
            $data[] = [
                'action' => $action,
                'created_at' => $now,
                'updated_at' => $now,
            ];
        }
        DB::table('permissions')->insert($data);
    }
}
