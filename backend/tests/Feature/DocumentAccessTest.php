<?php

namespace Tests\Feature;

use App\Models\Document;
use App\Models\Role;
use App\Models\User;
use Illuminate\Contracts\Auth\Authenticatable;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Foundation\Testing\WithoutMiddleware;
use Tests\TestCase;

class DocumentAccessTest extends TestCase
{
    use RefreshDatabase, WithoutMiddleware;

    protected $adminRole;
    protected $hrRole;
    protected $employeeRole;

    protected function setUp(): void
    {
        parent::setUp();

        $this->adminRole = Role::create(['name' => 'admin']);
        $this->hrRole = Role::create(['name' => 'hr']);
        $this->employeeRole = Role::create(['name' => 'employee']);
    }

    public function test_user_can_see_their_own_documents()
    {
        $user = User::factory()->createOne();
        assert($user instanceof Authenticatable);
        $user->roles()->attach($this->employeeRole);

        $document = Document::create([
            'title' => 'My Document',
            'created_by' => $user->id,
            'is_active' => true,
            'type' => 'pdf'
        ]);

        $response = $this->actingAs($user)->getJson('/api/documents');

        $response->assertStatus(200)
            ->assertJsonCount(1)
            ->assertJsonFragment(['title' => 'My Document']);
    }

    public function test_user_can_see_admin_and_hr_documents()
    {
        $admin = User::factory()->createOne();
        assert($admin instanceof Authenticatable);
        $admin->roles()->attach($this->adminRole);

        $hr = User::factory()->createOne();
        assert($hr instanceof Authenticatable);
        $hr->roles()->attach($this->hrRole);

        $user = User::factory()->createOne();
        assert($user instanceof Authenticatable);
        $user->roles()->attach($this->employeeRole);

        Document::create([
            'title' => 'Admin Document',
            'created_by' => $admin->id,
            'is_active' => true,
            'type' => 'pdf'
        ]);

        Document::create([
            'title' => 'HR Document',
            'created_by' => $hr->id,
            'is_active' => true,
            'type' => 'pdf'
        ]);

        $response = $this->actingAs($user)->getJson('/api/documents');

        $response->assertStatus(200)
            ->assertJsonCount(2)
            ->assertJsonFragment(['title' => 'Admin Document'])
            ->assertJsonFragment(['title' => 'HR Document']);
    }

    public function test_user_cannot_see_other_regular_users_documents()
    {
        $user1 = User::factory()->createOne();
        assert($user1 instanceof Authenticatable);
        $user1->roles()->attach($this->employeeRole);

        $user2 = User::factory()->createOne();
        assert($user2 instanceof Authenticatable);
        $user2->roles()->attach($this->employeeRole);

        Document::create([
            'title' => 'User 1 Document',
            'created_by' => $user1->id,
            'is_active' => true,
            'type' => 'pdf'
        ]);

        $response = $this->actingAs($user2)->getJson('/api/documents');

        $response->assertStatus(200)
            ->assertJsonCount(0);
    }

    public function test_user_can_only_see_accessible_favorited_documents()
    {
        $user1 = User::factory()->createOne();
        assert($user1 instanceof Authenticatable);
        $user1->roles()->attach($this->employeeRole);

        $admin = User::factory()->createOne();
        assert($admin instanceof Authenticatable);
        $admin->roles()->attach($this->adminRole);

        $doc1 = Document::create([
            'title' => 'My Fav',
            'created_by' => $user1->id,
            'is_active' => true,
            'type' => 'pdf'
        ]);

        $doc2 = Document::create([
            'title' => 'Admin Fav',
            'created_by' => $admin->id,
            'is_active' => true,
            'type' => 'pdf'
        ]);

        // Favorite both
        \App\Models\Favorite::create(['user_id' => $user1->id, 'document_id' => $doc1->id]);
        \App\Models\Favorite::create(['user_id' => $user1->id, 'document_id' => $doc2->id]);

        $response = $this->actingAs($user1)->getJson('/api/documents/favorites');

        $response->assertStatus(200)
            ->assertJsonCount(2)
            ->assertJsonFragment(['title' => 'My Fav'])
            ->assertJsonFragment(['title' => 'Admin Fav']);
    }
}
