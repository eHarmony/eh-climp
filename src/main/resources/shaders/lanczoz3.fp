#if __VERSION__ >= 130
  #define varying in
  out vec4 mgl_FragColor;
  #define texture2D texture
#else
  #define mgl_FragColor gl_FragColor
#endif


uniform sampler2D mgl_ActiveTexture;

uniform float destWidth;
uniform	float destHeight;

varying  vec2          mgl_texCoord;

// radius 2 actually subtracts a ring of border pixels, see lanczoz wikipedia
// radius 3 adds back in another ring of border pixels
// radius 3 is 36/16 = 2.25x more computationally expensive so be careful!
int SAMPLING_RADIUS = 3;
int MIN_SAMPLES=2 * SAMPLING_RADIUS;
int MAX_SAMPLES=16 * MIN_SAMPLES;
int CONVERT_TO_DIAMETER=2;

// how much do we want to abuse bilinear sampling in the gpu itself? Should be between 1.0 and 2.0
float SAMPLE_CHEAT_RATIO=1.5;


	 float sincModified(float value)
	{
		return sin(value) / value;
	}

	float apply(float value, float samplingRadius)
	{
		if (value==0){
			return 1.0;
		}
		if (value < 0.0f)
		{
			value = -value;
		}

		if (value <= samplingRadius)
		{
			value *= 3.14159;
			return sincModified(value) * sincModified(value / samplingRadius);
		}
		else
		{
			return 0.0f;
		}
	}


vec4 Lanczoz( sampler2D textureSampler, vec2 TexCoord, float fWidth, float fHeight )
{
	float samplingRadius = float(SAMPLING_RADIUS);
	float ratioX = destWidth/fWidth;
	float ratioY = destHeight/fHeight;

	float width = samplingRadius / ratioX;
	float height = samplingRadius / ratioY;
	// needs to be a factor of 2 since we go on both sides but since we use bilinear interpolation, we can sample up to 1/4 as many pixels
	// add 0.5 to round
	int numExtraSamplesX = CONVERT_TO_DIAMETER * int(width/SAMPLE_CHEAT_RATIO+0.5);
	int numExtraSamplesY = CONVERT_TO_DIAMETER * int(height/SAMPLE_CHEAT_RATIO+0.5);
	if (numExtraSamplesX > MAX_SAMPLES) {
		numExtraSamplesX = MAX_SAMPLES;
	}
	if (numExtraSamplesX < MIN_SAMPLES) {
		numExtraSamplesX = MIN_SAMPLES;
	}
	if (numExtraSamplesY > MAX_SAMPLES) {
		numExtraSamplesY = MAX_SAMPLES;
	}
	if (numExtraSamplesY < MIN_SAMPLES) {
		numExtraSamplesY = MIN_SAMPLES;
		}
	float fNormFacX = float(1.0 / fWidth);
	float fNormFacY = float(1.0 / fHeight);

	// back to radius units
	float halfSamplesX = float(numExtraSamplesX) / float(CONVERT_TO_DIAMETER);
	float halfSamplesY = float(numExtraSamplesY) / float(CONVERT_TO_DIAMETER);

    vec4 nSum = vec4( 0.0, 0.0, 0.0, 0.0 );
    vec4 nDenom = vec4( 0.0, 0.0, 0.0, 0.0 );

	float left = TexCoord.x - width / fWidth;
	float bottom = TexCoord.y - height / fHeight;
	float right = TexCoord.x + width / fWidth;
	float top = TexCoord.y + height / fHeight;
	float jumpX = (right - left) / float(numExtraSamplesX);
	float jumpY = (top - bottom) / float(numExtraSamplesY);

	//TODO make this use MAX_SAMPLES somehow
	float yJumps[32];
	float yWeights[32];
	for( int n = 0 ; n < numExtraSamplesY; n += 1)
	{
		// Uniform distribution (see wikipedia supersampling)
		// we start from the left side, go an even number of jumps. Start half a pixel from the bottom left
    	yWeights[n] = apply( samplingRadius * (0.5 + float(n) - halfSamplesY)/halfSamplesY, samplingRadius );
    	yJumps[n] = bottom + jumpY * (float( n) +0.5);
    }

    for( int m = 0; m < numExtraSamplesX; m += 1 )
    {
        float f  = apply( samplingRadius * (0.5 + float(m) - halfSamplesX)/halfSamplesX, samplingRadius );
        vec4 vecCooef1 = vec4( f,f,f,f );
        float currentX = left + jumpX * (float(m) + 0.5);
        for( int n = 0 ; n < numExtraSamplesY; n += 1)
        {

        	vec2 TexOffset = vec2( currentX , yJumps[n]) ;
            vec4 vecData = texture2D(textureSampler, TexOffset);

            float f1 = yWeights[n];
            vec4 vecCoeef2 = vec4( f1, f1, f1, f1 );

            vec4 weight = vecCoeef2 * vecCooef1;
            nSum = nSum +  vecData * weight ;
            nDenom = nDenom + weight;
        }
    }
    return nSum / nDenom;
}


void main()
{
	vec2 texSize = textureSize(mgl_ActiveTexture, 0);
    vec4 Data = Lanczoz( mgl_ActiveTexture, mgl_texCoord.st, texSize.x, texSize.y );
    mgl_FragColor = Data;
}