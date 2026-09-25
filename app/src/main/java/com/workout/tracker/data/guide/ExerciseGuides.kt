package com.workout.tracker.data.guide

import com.workout.tracker.domain.model.ExerciseGuide
import javax.inject.Inject
import javax.inject.Singleton

/** Exercise guides, keyed by exercise name. Edit [GUIDES] to change the content. */
@Singleton
class ExerciseGuides @Inject constructor() {

    fun forName(name: String): ExerciseGuide? = GUIDES[name]

    fun hasGuide(name: String): Boolean = GUIDES.containsKey(name)

    private companion object {
        val GUIDES: Map<String, ExerciseGuide> = mapOf(

            "Bench Press" to ExerciseGuide(
                summary = "The main horizontal pressing movement for the chest, front " +
                    "delts and triceps, and the primary upper-body strength lift in this program.",
                steps = listOf(
                    "Lie on the bench with your eyes directly under the bar and both feet flat on the floor.",
                    "Pull your shoulder blades back and down into the bench and keep them pinned there.",
                    "Grip the bar slightly wider than shoulder width and unrack it to over your shoulders.",
                    "Lower the bar under control to your mid-chest, elbows about 45 degrees from your torso.",
                    "Press back up to lockout without letting your shoulders roll forward.",
                ),
                tips = listOf(
                    "Keep your wrists stacked over your forearms rather than bent back.",
                    "Drive your feet into the floor to stay tight through the whole set.",
                    "Use a spotter or safety pins whenever you work close to failure.",
                ),
                musclesWorked = "Chest, front deltoids, triceps",
            ),

            "Bent-Over Barbell Row" to ExerciseGuide(
                summary = "A horizontal pull that builds mid-back thickness and balances out " +
                    "all the pressing volume in the program.",
                steps = listOf(
                    "Stand with feet hip-width and the bar over mid-foot, then hinge at the hips until your torso is around 45 degrees or lower.",
                    "Grip the bar just outside your knees with a flat back and a braced core.",
                    "Pull the bar to your lower ribs, driving your elbows back rather than flaring them out.",
                    "Squeeze your shoulder blades together at the top, then lower under control.",
                ),
                tips = listOf(
                    "Keep your torso angle fixed. Standing up to move the weight turns it into a different exercise.",
                    "Look at the floor a few feet ahead of you to keep your neck neutral.",
                ),
                musclesWorked = "Lats, rhomboids, mid traps, rear deltoids, biceps",
            ),

            "Standing Overhead Press" to ExerciseGuide(
                summary = "A vertical press that builds shoulder strength and overhead " +
                    "stability, which carries over directly to lifting and carrying loads overhead.",
                steps = listOf(
                    "Set the bar at collarbone height with a grip just outside shoulder width.",
                    "Brace your core and squeeze your glutes so your ribs stay down.",
                    "Press the bar straight up, moving your head back slightly to clear a path.",
                    "Once the bar clears your forehead, push your head through so the bar finishes over your mid-foot.",
                    "Lower under control back to your collarbones.",
                ),
                tips = listOf(
                    "Do not lean back. The bar path should be vertical, not an incline press.",
                    "Keep your forearms vertical throughout the press.",
                ),
                musclesWorked = "Deltoids, triceps, upper chest, core",
            ),

            "Pull-ups / Lat Pulldown" to ExerciseGuide(
                summary = "The main vertical pull. Use pull-ups if you can manage the target " +
                    "reps, and the lat pulldown to build up to them or to add volume.",
                steps = listOf(
                    "Grip the bar slightly wider than shoulder width with your palms facing away.",
                    "Start from a full hang with your shoulders active rather than fully relaxed.",
                    "Pull your elbows down and back until your chin clears the bar.",
                    "Lower under control all the way back to a full hang. That is one rep.",
                ),
                tips = listOf(
                    "Keep it strict. Kipping and swinging inflate the rep count without building the strength.",
                    "On the pulldown, stay close to upright instead of leaning back to cheat the weight.",
                    "Log assisted or banded reps as normal reps and record the assistance in the notes.",
                ),
                musclesWorked = "Lats, biceps, rear deltoids, mid back",
            ),

            "Push-ups" to ExerciseGuide(
                summary = "A bodyweight horizontal press. It is tested in most military PT " +
                    "standards, which is why it appears twice in this program.",
                steps = listOf(
                    "Set your hands slightly wider than shoulder width, directly under your chest.",
                    "Form a straight line from head to heels and brace your core and glutes.",
                    "Lower until your chest is roughly a fist height from the floor, elbows at about 45 degrees.",
                    "Press back up to full lockout without letting your hips sag or pike.",
                ),
                tips = listOf(
                    "Full range of motion is worth more than extra partial reps.",
                    "If your lower back arches, brace harder or move to an incline until you can hold position.",
                ),
                musclesWorked = "Chest, front deltoids, triceps, core",
            ),

            "Plank" to ExerciseGuide(
                summary = "An isometric core hold. You are training the core to resist " +
                    "movement rather than to create it.",
                steps = listOf(
                    "Set your forearms on the floor with your elbows directly under your shoulders.",
                    "Extend your legs back and come up onto your toes.",
                    "Squeeze your glutes and brace your abs so your body forms a straight line.",
                    "Hold for the target time, breathing steadily rather than holding your breath.",
                ),
                tips = listOf(
                    "Do not let your hips drift up or sag down.",
                    "If your lower back starts to ache, end the set. Quality beats duration here.",
                    "Log the difficulty honestly so the progression actually means something.",
                ),
                musclesWorked = "Abs, obliques, deep core, shoulders",
            ),

            "Incline Treadmill Walk" to ExerciseGuide(
                summary = "Low-impact aerobic work on an incline. Builds the aerobic base " +
                    "without the joint cost of running.",
                steps = listOf(
                    "Set the treadmill to a brisk walking pace with a meaningful incline, starting around 8 to 10 percent.",
                    "Walk without holding the handrails, using them only briefly for balance.",
                    "Hold an effort where you could speak in short sentences but not comfortably hold a conversation.",
                    "Log the total time and distance when you finish.",
                ),
                tips = listOf(
                    "Raise the incline before the speed if you want it harder.",
                    "Holding the rails takes most of the work out of the session.",
                ),
                musclesWorked = "Calves, glutes, hamstrings, cardiovascular system",
            ),

            "Back Squat" to ExerciseGuide(
                summary = "The primary lower-body strength lift. Builds the legs and hips and " +
                    "loads the whole posterior chain.",
                steps = listOf(
                    "Set the bar across your upper traps and rear delts, not your neck, and grip it firmly.",
                    "Unrack and step back into a stance a little wider than your shoulders with toes slightly out.",
                    "Take a big breath, brace your core, then sit down and back.",
                    "Descend until your hip crease is at or below your knee while keeping your chest up.",
                    "Drive through mid-foot to stand back up.",
                ),
                tips = listOf(
                    "Track your knees out over your toes and do not let them collapse inward.",
                    "Keep your weight over mid-foot rather than drifting onto your toes.",
                    "Set the safety pins just below your bottom position before you start.",
                ),
                musclesWorked = "Quads, glutes, adductors, spinal erectors, core",
            ),

            "Romanian Deadlift" to ExerciseGuide(
                summary = "A hip hinge that loads the hamstrings and glutes through a stretch. " +
                    "Unlike a deadlift, the bar never rests on the floor between reps.",
                steps = listOf(
                    "Start standing, holding the bar at your hips with a shoulder-width grip.",
                    "Soften your knees slightly and hold that same knee angle for the whole rep.",
                    "Push your hips back and let the bar slide down your thighs, keeping it close to your legs.",
                    "Stop when you feel a strong hamstring stretch, usually around mid-shin, with a flat back.",
                    "Drive your hips forward to stand back up.",
                ),
                tips = listOf(
                    "This is a hip movement, not a knee movement. The knees stay quiet.",
                    "Your back should stay flat. The moment it rounds is the end of your range.",
                ),
                musclesWorked = "Hamstrings, glutes, spinal erectors, lats",
            ),

            "Walking Lunges" to ExerciseGuide(
                summary = "A single-leg movement that builds leg strength while exposing " +
                    "side-to-side imbalances.",
                steps = listOf(
                    "Stand tall holding dumbbells at your sides, or with a bar on your back.",
                    "Step forward far enough that your front shin finishes roughly vertical.",
                    "Lower until your back knee is just above the floor.",
                    "Drive through your front heel and step straight into the next rep with the other leg.",
                ),
                tips = listOf(
                    "Keep your torso upright. Leaning forward shifts the work off the target muscles.",
                    "The prescription is per leg, so a 10 per leg target is 20 total steps.",
                ),
                musclesWorked = "Quads, glutes, hamstrings, adductors, core",
            ),

            "Step-ups" to ExerciseGuide(
                summary = "A single-leg step onto a raised platform. One of the most direct " +
                    "carryovers to rucking, hills and stairs under load.",
                steps = listOf(
                    "Set a box or bench at roughly knee height.",
                    "Place one whole foot on the box while holding dumbbells at your sides.",
                    "Drive through the heel of the top foot to stand up fully on the box.",
                    "Lower yourself under control rather than dropping or bouncing off the floor.",
                ),
                tips = listOf(
                    "Push through the top leg. If you are pushing off the bottom foot, the box is too high.",
                    "Stand all the way up at the top with your hips fully extended.",
                ),
                musclesWorked = "Quads, glutes, hamstrings, calves",
            ),

            "Calf Raises" to ExerciseGuide(
                summary = "Direct calf work that supports running, rucking and ankle resilience.",
                steps = listOf(
                    "Stand with the balls of your feet on a step or plate and your heels hanging free.",
                    "Let your heels drop below the step for a full stretch.",
                    "Push up onto your toes as high as you can.",
                    "Pause briefly at the top, then lower slowly.",
                ),
                tips = listOf(
                    "Slow the lowering phase down. That is where most of the benefit comes from.",
                    "Full range beats heavy weight with a short bounce.",
                ),
                musclesWorked = "Gastrocnemius, soleus",
            ),

            "Dead Bugs" to ExerciseGuide(
                summary = "A core exercise that trains you to keep your spine stable while your " +
                    "arms and legs move independently.",
                steps = listOf(
                    "Lie on your back with your arms straight up and your hips and knees bent to 90 degrees.",
                    "Press your lower back flat into the floor and keep it there for every rep.",
                    "Slowly extend your right arm overhead and your left leg straight out.",
                    "Return to the start under control, then repeat on the other side.",
                ),
                tips = listOf(
                    "If your lower back lifts off the floor, shorten the range until it does not.",
                    "Move slowly. Speed defeats the point of the exercise.",
                    "A 10 per side target means 10 on each side.",
                ),
                musclesWorked = "Deep core, abs, hip flexors",
            ),

            "Easy Run" to ExerciseGuide(
                summary = "Steady aerobic running at conversational pace. This builds the " +
                    "aerobic base that the rest of the conditioning work sits on.",
                steps = listOf(
                    "Start with a few minutes of easy jogging to warm up.",
                    "Settle into a pace where you could comfortably hold a conversation.",
                    "Keep your cadence quick and your stride relaxed.",
                    "Log the total distance and time when you finish.",
                ),
                tips = listOf(
                    "Easy days should genuinely feel easy. Going too hard here compromises the hard sessions.",
                    "If you cannot speak in full sentences, slow down.",
                ),
                musclesWorked = "Legs, hips, cardiovascular system",
            ),

            "Incline DB Bench Press" to ExerciseGuide(
                summary = "An upper-chest focused press. Dumbbells allow a longer range of " +
                    "motion and even out side-to-side differences.",
                steps = listOf(
                    "Set the bench to roughly 30 degrees.",
                    "Start with the dumbbells at shoulder level, palms facing forward.",
                    "Press up and slightly together until your arms are extended.",
                    "Lower under control until you feel a stretch across your chest.",
                ),
                tips = listOf(
                    "A steeper bench shifts the work onto your shoulders instead of your chest.",
                    "Keep your shoulder blades pinned back against the bench.",
                ),
                musclesWorked = "Upper chest, front deltoids, triceps",
            ),

            "Seated Cable Row" to ExerciseGuide(
                summary = "A controlled horizontal pull that is easier on the lower back than " +
                    "bent-over rows, so it pairs well with a heavy pressing day.",
                steps = listOf(
                    "Sit with your feet braced and a slight bend in your knees.",
                    "Sit tall with your chest up and your arms extended.",
                    "Pull the handle to your lower ribs, driving your elbows back.",
                    "Squeeze your shoulder blades together, then extend your arms under control.",
                ),
                tips = listOf(
                    "Keep your torso still. Rocking back and forth just moves the stack with momentum.",
                    "Let your shoulder blades travel forward at the end of each rep for a full stretch.",
                ),
                musclesWorked = "Lats, rhomboids, mid traps, rear deltoids, biceps",
            ),

            "Face Pulls" to ExerciseGuide(
                summary = "A high pull to the face that targets the rear delts and upper back. " +
                    "Balances out pressing volume and supports shoulder health.",
                steps = listOf(
                    "Set a rope attachment at roughly face height.",
                    "Grip the rope with your thumbs pointing back and step back to create tension.",
                    "Pull the rope toward your face, separating your hands as you go.",
                    "Finish with your hands beside your ears and your elbows high, then return under control.",
                ),
                tips = listOf(
                    "Go lighter than you think you need to. This is about position, not load.",
                    "Keep your elbows at or above shoulder height throughout.",
                ),
                musclesWorked = "Rear deltoids, rotator cuff, mid traps, rhomboids",
            ),

            "Farmer Carries" to ExerciseGuide(
                summary = "Walking while holding heavy weight. One of the most direct " +
                    "carryovers to carrying gear and equipment.",
                steps = listOf(
                    "Set a heavy dumbbell or kettlebell on each side of you.",
                    "Hinge down, grip hard, and stand up tall with your shoulders back.",
                    "Walk with short, controlled steps while keeping your torso upright.",
                    "Keep walking for the target time, then set the weights down under control.",
                ),
                tips = listOf(
                    "Do not let your shoulders round forward or the weights swing.",
                    "Your grip will usually fail before your legs do. That is the point of the exercise.",
                ),
                musclesWorked = "Grip, forearms, traps, core, glutes, quads",
            ),

            "Hanging Knee Raises" to ExerciseGuide(
                summary = "Core work from a dead hang. Trains the abs and hip flexors while " +
                    "adding grip endurance.",
                steps = listOf(
                    "Hang from a pull-up bar with an overhand grip and active shoulders.",
                    "Without swinging, raise your knees toward your chest.",
                    "Pause briefly at the top with your pelvis tucked slightly under.",
                    "Lower under control back to a full hang.",
                ),
                tips = listOf(
                    "Control beats height. Swinging turns this into a momentum exercise.",
                    "Progress to straight legs once knee raises become easy.",
                ),
                musclesWorked = "Abs, hip flexors, obliques, grip",
            ),

            "200m Intervals" to ExerciseGuide(
                summary = "Short, hard running repeats with recovery between them. Builds speed " +
                    "and anaerobic capacity.",
                steps = listOf(
                    "Warm up with about 10 minutes of easy jogging and a few strides.",
                    "Run 200m hard but controlled, rather than all-out on the first rep.",
                    "Recover with 60 to 90 seconds of walking or easy jogging.",
                    "Repeat for the target number of reps, then cool down.",
                ),
                tips = listOf(
                    "Aim for even splits. If the last rep is far slower, you started too fast.",
                    "Log the total distance covered and the total working time.",
                ),
                musclesWorked = "Legs, hips, cardiovascular and anaerobic systems",
            ),

            "Deadlift" to ExerciseGuide(
                summary = "The heaviest pull in the program. Trains the whole posterior chain " +
                    "to lift a load off the ground.",
                steps = listOf(
                    "Stand with feet hip-width and the bar over mid-foot, shins close to the bar.",
                    "Hinge down and grip the bar just outside your legs.",
                    "Drop your hips until your shoulders are slightly ahead of the bar, chest up and back flat.",
                    "Take a breath, brace, and push the floor away, dragging the bar up your legs.",
                    "Lock out by standing tall. Do not lean back at the top.",
                ),
                tips = listOf(
                    "If your hips shoot up first, the weight is too heavy or your brace was loose.",
                    "Reset your position between reps instead of bouncing the bar off the floor.",
                    "Rounded backs are where deadlift injuries come from. End the set when your form breaks.",
                ),
                musclesWorked = "Glutes, hamstrings, spinal erectors, lats, traps, grip",
            ),

            "Bulgarian Split Squat" to ExerciseGuide(
                summary = "A rear-foot-elevated split squat. Brutal for the amount of weight " +
                    "involved and excellent for single-leg strength.",
                steps = listOf(
                    "Stand a couple of feet in front of a bench and place the top of your rear foot on it.",
                    "Hold dumbbells at your sides and set your front foot far enough forward to stay vertical at the bottom.",
                    "Lower straight down until your rear knee approaches the floor.",
                    "Drive through your front heel to stand back up.",
                ),
                tips = listOf(
                    "Almost all the work should be in the front leg. The back foot is only for balance.",
                    "Leaning slightly forward emphasises the glutes, staying upright hits the quads.",
                ),
                musclesWorked = "Quads, glutes, adductors, hamstrings",
            ),

            "Hip Thrust" to ExerciseGuide(
                summary = "A loaded hip extension. The most direct glute exercise in the " +
                    "program, and it supports sprinting and jumping.",
                steps = listOf(
                    "Sit on the floor with your upper back against a bench and a padded bar across your hips.",
                    "Plant your feet flat at roughly shoulder width so your shins are vertical at the top.",
                    "Drive through your heels and extend your hips until your torso is parallel to the floor.",
                    "Squeeze your glutes hard at the top, then lower under control.",
                ),
                tips = listOf(
                    "Keep your ribs down and chin tucked. Do not arch your lower back to finish the rep.",
                    "A brief pause at the top is worth more than adding weight.",
                ),
                musclesWorked = "Glutes, hamstrings, quads",
            ),

            "Conditioning Circuit" to ExerciseGuide(
                summary = "A timed circuit of mixed movements performed back to back with " +
                    "minimal rest. Trains work capacity under fatigue.",
                steps = listOf(
                    "Pick three to five movements you can perform safely when tired, such as carries, sled work, kettlebell swings, burpees or rows.",
                    "Work through them back to back with little or no rest between movements.",
                    "Rest between rounds, then repeat for the target number of rounds.",
                    "Log the total working time and how hard it felt.",
                ),
                tips = listOf(
                    "Choose movements that stay safe once your form degrades. Save technical lifts for the strength work.",
                    "Keep the circuit consistent week to week so the times are comparable.",
                ),
                musclesWorked = "Full body, cardiovascular and anaerobic systems",
            ),
        )
    }
}
