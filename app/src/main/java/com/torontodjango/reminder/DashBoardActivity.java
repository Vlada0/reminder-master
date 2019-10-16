package com.torontodjango.reminder;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarDrawerToggle;import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;


class TaskListAdapter extends BaseAdapter{

    private final String TAG = "TaskListAdapter";

    static class ViewHolder
    {
        TextView name;
        TextView time;
        TextView date;
    }

    private Context context;
    private DAO dao; // access to data
    private LayoutInflater inflater;


    private AlarmManager alarmManager;

    public TaskListAdapter (Context context)
    {
        Log.d(TAG, "calling constructor, context is " + (context!=null ? "not null" : "null"));

        this.context = context;
        dao = DAO.getInstance(context);

        inflater = LayoutInflater.from(context);

        alarmManager = (AlarmManager)context.getSystemService(context.ALARM_SERVICE);
    }

    public View getView(int position, View convertView, ViewGroup parent)
    {
        ViewHolder holder;
        Task task = dao.get(position);

        if (convertView == null)
        {
            convertView = inflater.inflate(R.layout.record, null);

            holder = new ViewHolder();
            holder.name = (TextView)convertView.findViewById(R.id.record_name);
            holder.time = (TextView)convertView.findViewById(R.id.record_time);
            holder.date = (TextView)convertView.findViewById(R.id.record_date);

            convertView.setTag(holder);
        }
        else
        {
            holder = (ViewHolder)convertView.getTag();
        }

        holder.name.setText(task.getName());
        holder.time.setText(DAO.formatTime(task));
        holder.date.setText(DAO.formatDate(task));

        if (task.getOutdated() || !task.getEnabled())
            holder.name.setTextColor(context.getResources().getColor(R.color.outdated));
        else
            holder.name.setTextColor(context.getResources().getColor(R.color.active));

        return convertView;
    }

    public int getCount()
    {
        return dao.size();
    }

    public Task getItem(int position)
    {
        return dao.get(position);
    }

    public long getItemId(int position)
    {
        return position;
    }

    public void add(Task Task)
    {
        dao.add(Task);
        update();
    }

    public void delete(int index)
    {
        cancelTask(dao.get(index));
        //dao.remove(index);
        update();
    }

    public void update(Task Task)
    {
        dao.update(Task);
        update();
    }

    private void update()
    {
        for (int i = 0; i < dao.size(); i++)
            setTask(dao.get(i));

        notifyDataSetChanged();
    }

    private void setTask(Task task){
        PendingIntent sender;
        Intent intent;

        Log.d(TAG, "Setting task to alarm at " + task.getDate() + (task.getEnabled() ? " enabled" : "") + (task.getOutdated() ? " outdated" : ""));

        if (task.getEnabled() && !task.getOutdated())
        {
            intent = new Intent(context, TaskReceiver.class);

            task.toIntent(intent);
            sender = PendingIntent.getBroadcast(context, (int)task.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

            //dao.remove(task);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, task.getDate(), sender);

        }
    }

    private void cancTask(Task task){
        PendingIntent sender;
        Intent intent;

        Log.d(TAG, "Setting task to alarm at " + task.getDate() + (task.getEnabled() ? " enabled" : "") + (task.getOutdated() ? " outdated" : ""));

        if (task.getEnabled() && !task.getOutdated())
        {
            intent = new Intent(context, TaskReceiver.class);
            task.toIntent(intent);
            sender = PendingIntent.getBroadcast(context, (int)task.getId(), intent, PendingIntent.FLAG_CANCEL_CURRENT);
            // alarmManager.setExact(AlarmManager.RTC_WAKEUP, task.getDate(), sender);
            sender.cancel();

        }
    }

    private void cancelTask(Task task){
//Task task1=task;
        cancTask(task);

        dao.remove(task);
        update();
    }

}




public class DashBoardActivity extends AppCompatActivity implements TasksFragment.OnFragmentInteractionListener{

    private final String TAG = "DashBoardActivity";

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;

    private TaskListAdapter taskListAdapter;
    private Task currentTask;

    private final int NEW_ACTIVITY = 0;
    private final int EDIT_ACTIVITY = 1;

    private final int CONTEXT_MENU_EDIT = 0;
    private final int CONTEXT_MENU_DELETE = 1;
    private static SimpleDateFormat timeFormat = new SimpleDateFormat("H:mm");
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("E MMM d, yyyy");
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d(TAG, "starting dashboard activity");
        final EditText editText;

        editText = findViewById(R.id.editText);
        Button search_button;
        search_button = findViewById(R.id.search_button);
        taskListAdapter = new TaskListAdapter(this);
        currentTask = null;


//search_button.setOnClickListener(new View.OnClickListener() {
   // @Override
   // public void onClick(View view) {
     // Search(editText.getText().toString());
   // }
//});
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dash_board);

        if (!taskListAdapter.isEmpty())
        {
            for (int i=0; i<taskListAdapter.getCount(); i++){
                if(taskListAdapter.getItem(i).getDate()<System.currentTimeMillis())
                    taskListAdapter.delete(i);
            }
        }
        drawerLayout = (DrawerLayout)findViewById(R.id.activity_main);
        toggle = new ActionBarDrawerToggle(this, drawerLayout,R.string.open, R.string.close);

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        navigationView = (NavigationView)findViewById(R.id.nv);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                int id = item.getItemId();
                Class fragmentClass = TasksFragment.class;
                switch(id)
                {
                    case R.id.dashboard:
                        Log.d(TAG, "Choosing tasks");

                        break;
                    case R.id.help:
                        fragmentClass = HelpFragment.class;
                        Log.d(TAG, "Choosing help");
                        //Intent intent = new Intent(getBaseContext(), HelpActivity.class);
                        //DashBoardActivity.this.startActivity(intent);
                        break;
                    case R.id.exit:
                        finish();
                        moveTaskToBack(true);
                        break;
                }
                switchFragment(fragmentClass);

                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });

        switchFragment(TasksFragment.class);


    }



    void switchFragment(Class fragmentClass){
        Fragment fragment = null;

        try {
            fragment = (Fragment) fragmentClass.newInstance();
        }
        catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.tasksfragment, fragment).commit();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(toggle.onOptionsItemSelected(item))
            return true;

        return super.onOptionsItemSelected(item);
    }

    public TaskListAdapter getTaskListAdapter(){
        return taskListAdapter;
    }

    public void onFragmentInteraction(int position){
        Intent intent = new Intent(getBaseContext(), EditActivity.class);

        currentTask = taskListAdapter.getItem(position);
        currentTask.toIntent(intent);
        DashBoardActivity.this.startActivityForResult(intent, EDIT_ACTIVITY);
    }
    public static String formatTime(Task Task)
    {
        return timeFormat.format(new Date(Task.getDate()));
    }

    public static String formatDate(Task Task)
    {
        return dateFormat.format(new Date(Task.getDate()));
    }
    public void Search(View view){
        String name ="";
        EditText editText;
        editText = findViewById(R.id.editText);
        name = editText.getText().toString();
        Task task = new Task();
        TextView textView;
        textView = findViewById(R.id.textView);
        String Data = "";
        String Time = "";
        String str1 = "";
        String Description = "";
        for (int i = 0; i < taskListAdapter.getCount(); i++) {
            str1 = taskListAdapter.getItem(i).getName();

            if (str1.equals(name)) {

                Data = formatDate(taskListAdapter.getItem(i));
               Time = formatTime(taskListAdapter.getItem(i));
                //Time = String.valueOf(Data);


                Description = taskListAdapter.getItem(i).getName();
                textView.setText(Data+" " +Time + " " + Description);
            }
        }
    }

    public void onAddClick(View view)
    {
        Intent intent = new Intent(getBaseContext(), EditActivity.class);

        currentTask = new Task();
        currentTask.toIntent(intent);

        DashBoardActivity.this.startActivityForResult(intent, NEW_ACTIVITY);
    }


    public void onEditClick(View view){
        View parentRow = (View) view.getParent();
        ListView listView = (ListView) parentRow.getParent().getParent();
        final int position = listView.getPositionForView(parentRow);

        Intent intent = new Intent(getBaseContext(), EditActivity.class);
        currentTask = taskListAdapter.getItem(position);
        currentTask.toIntent(intent);
        startActivityForResult(intent, EDIT_ACTIVITY);
    }

    public void onDeleteClick(View view){
        View parentRow = (View) view.getParent();
        ListView listView = (ListView) parentRow.getParent().getParent();
        final int position = listView.getPositionForView(parentRow);
        taskListAdapter.delete(position);


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == NEW_ACTIVITY || requestCode == EDIT_ACTIVITY)
        {
            if (resultCode == RESULT_OK)
            {
                currentTask.fromIntent(data);
                if(requestCode == NEW_ACTIVITY)
                    taskListAdapter.add(currentTask);
                else
                    taskListAdapter.update(currentTask);
            }
            currentTask = null;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
        if (v.getId() == R.id.task_list)
        {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;

            menu.setHeaderTitle(taskListAdapter.getItem(info.position).getName());
            menu.add(Menu.NONE, CONTEXT_MENU_EDIT, Menu.NONE, "Edit");
            menu.add(Menu.NONE, CONTEXT_MENU_DELETE, Menu.NONE, "Delete");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        int index = item.getItemId();

        if (index == CONTEXT_MENU_EDIT)
        {
            Intent intent = new Intent(getBaseContext(), EditActivity.class);

            currentTask = taskListAdapter.getItem(info.position);
            currentTask.toIntent(intent);
            startActivityForResult(intent, EDIT_ACTIVITY);
        }
        else if (index == CONTEXT_MENU_DELETE)
        {
            taskListAdapter.delete(info.position);
        }

        return true;
    }

    private AdapterView.OnItemClickListener listOnItemClickListener = new AdapterView.OnItemClickListener()
    {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
            Intent intent = new Intent(getBaseContext(), EditActivity.class);

            currentTask = taskListAdapter.getItem(position);
            currentTask.toIntent(intent);
            DashBoardActivity.this.startActivityForResult(intent, EDIT_ACTIVITY);
        }
    };



}
